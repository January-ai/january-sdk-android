"""Local-only API fixtures shared by the real Android and iOS demo workflows.
No credentials and no proxying to production. Control routes are test-only.
"""
import json, time, threading
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from pathlib import Path

NUTRIENTS = {k: {'value':v,'unit':u} for k,v,u in [('calories',100,'kcal'),('protein',4,'g'),('carbohydrates',20,'g'),('total_fat',2,'g'),('fiber',3,'g'),('sodium',10,'mg')]}
SERVINGS = [dict(id='11',quantity=1,unit='cup',scaling_factor=1,weight_grams=100,is_primary=True),dict(id='12',quantity=1,unit='oz',scaling_factor=0.2835,weight_grams=28.35,is_primary=False)]
def food(id='101',name='Fixture oatmeal',full=True):return dict(id=str(id),type='generic',name=name,brand_name='January fixture',nutrients=NUTRIENTS,glycemic_index=52,glycemic_load=12,image_url=None,barcode=None,servings=SERVINGS if full else SERVINGS[:1])
PREDICTION = dict(points=[dict(minutes=m,value=v) for m,v in [(0,90),(30,125),(60,140),(90,115),(120,95)]],impact_score='medium',chart=dict(min=70,max=140))
def scan(name='Fixture breakfast'):return dict(meal_name=name,detections=[dict(food=food(),confidence='high')],total_nutrients=NUTRIENTS)
def log(name='Fixture breakfast'):
 f=food(); f.pop('servings');f.pop('type');f.pop('barcode');f.update(food_id=f.pop('id'),quantity=1,serving=dict(id='11',quantity=1,unit='cup',weight_grams=100))
 return dict(id='11111111-1111-4111-8111-111111111111',name=name,eaten_at='2026-08-31T12:00:00Z',foods=[f])
state={'rules':{},'logs':[],'requests':[]}
class Handler(BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_GET(self):self.handle_request()
 def do_POST(self):self.handle_request()
 def do_PATCH(self):self.handle_request()
 def do_PUT(self):self.handle_request()
 def do_DELETE(self):self.handle_request()
 def handle_request(self):
  parsed=urlparse(self.path);path=parsed.path;q={k:v[0] for k,v in parse_qs(parsed.query).items()}
  raw=self.rfile.read(int(self.headers.get('Content-Length',0)))
  body=json.loads(raw) if raw and 'application/json' in self.headers.get('Content-Type','') else {}
  if path=='/__reset':state.update(rules={},logs=[],requests=[]);return self.respond({})
  if path=='/__control':state['rules'][q['route']]=q;return self.respond({})
  if path=='/__seed':state['logs']=[log()];return self.respond({})
  if path=='/__requests':return self.respond(state['requests'])
  state['requests'].append(dict(method=self.command,path=path,query=q,body=body))
  rule=state['rules'].get(path,{})
  delay=float(rule.get('delay',0))
  if delay:time.sleep(delay)
  status=int(rule.get('status',200));empty=rule.get('empty')=='true'
  if status!=200:
   if status==404 and '/restaurants/' in path and path.endswith('/menu-items'):
    restaurant_id=path.split('/restaurants/',1)[1].split('/menu-items',1)[0]
    return self.respond(dict(code='not_found',message='No restaurant with id '+restaurant_id+'. Use an id from a GET /v1.2/restaurants result.'),status)
   return self.respond(dict(code='fixture_error',message='The test request could not be completed.',request_id='parity-request',docs_url='https://example.invalid/fixture-docs'),status)
  if path.endswith('/autocomplete'):result=dict(items=[])
  elif path.endswith('/alternatives'):result=dict(alternatives=[] if empty else [food('102','Fixture lentils')])
  elif path.endswith('/foods/101'):result=food()
  elif path.endswith('/foods/102'):result=food(102,'Fixture lentils')
  elif path.endswith('/foods'):result=dict(items=[] if empty else [food(full=False)])
  elif '/foods/barcode/' in path:result=food(full=False)
  elif path.endswith('/restaurants/cafe/menu-items'):result=dict(items=[] if empty or int(q.get('offset',0)) > 0 else [dict(id='101',name='Fixture bowl',nutrients=NUTRIENTS,servings=SERVINGS),dict(id='102',name='Fixture soup',nutrients=NUTRIENTS,servings=SERVINGS)])
  elif path.endswith('/menu-items') and '/restaurants/' not in path:
   restaurant_name='Fixture Cafe'
   result=dict(items=[] if empty else [dict(type='menu_item',id='101',name='Fixture bowl',restaurant_name=restaurant_name,is_chain=False,distance_meters=100,image_url=None,nutrients=NUTRIENTS,glycemic_index=None,glycemic_load=None,servings=SERVINGS),dict(type='menu_item',id='102',name='Fixture soup',restaurant_name=restaurant_name,is_chain=False,distance_meters=100,image_url=None,nutrients=NUTRIENTS,glycemic_index=None,glycemic_load=None,servings=SERVINGS)])
  elif path.endswith('/restaurants'):result=dict(items=[] if empty else [dict(type='restaurant',id='cafe',name='Fixture Cafe',city='San Francisco',address1='123 Test Street',address2=None,is_chain=False,distance_meters=100)])
  elif path.endswith('/glucose/predictions'):result=PREDICTION
  elif path.endswith('/food-analysis/image'):result=scan()
  elif path.endswith('/food-analysis/corrections'):result=scan('Corrected breakfast')
  elif path.endswith('/food-analysis/text'):result=dict(meal_name=None,detections=[] if empty else [dict(food=food(),confidence=None)],total_nutrients=NUTRIENTS)
  elif '/food-logs' in path:
   if self.command=='GET':result=dict(items=state['logs'])
   elif self.command=='DELETE':state['logs']=[];result=dict(status='success')
   else:
    result=log(body.get('name') or 'Fixture breakfast');state['logs']=[result]
  else:return self.respond(dict(message='Unmapped fixture route '+path),404)
  self.respond(result)
 def respond(self,body,status=200):
  data=json.dumps(body).encode();self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(data)));self.end_headers()
  try:self.wfile.write(data)
  except (BrokenPipeError,ConnectionResetError):pass
if __name__=='__main__':ThreadingHTTPServer(('127.0.0.1',int(__import__('sys').argv[1]) if len(__import__('sys').argv)>1 else 18765),Handler).serve_forever()
