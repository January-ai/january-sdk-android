"""Local-only API fixtures shared by the real Android and iOS demo workflows.
No credentials and no proxying to production. Control routes are test-only.
"""
import json, time, threading
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from pathlib import Path

NUTRIENTS = {k: {'value':v,'unit':u} for k,v,u in [('calories',100,'kcal'),('protein',4,'g'),('carbohydrates',20,'g'),('total_fat',2,'g'),('fiber',3,'g'),('sodium',10,'mg')]}
SERVINGS = [dict(id=11,quantity=1,unit='cup',scaling_factor=1,weight_grams=100,is_primary=True),dict(id=12,quantity=1,unit='oz',scaling_factor=0.2835,weight_grams=28.35,is_primary=False)]
def food(id=101,name='Fixture oatmeal',full=True):return dict(id=id,name=name,brand_name='January fixture',nutrients=NUTRIENTS,servings=SERVINGS if full else SERVINGS[:1])
PREDICTION = dict(prediction=[dict(minutes=m,value=v) for m,v in [(0,90),(30,125),(60,140),(90,115),(120,95)]],impact_score='medium',chart=dict(min=70,max=140))
def scan(name='Fixture breakfast'):return dict(meal_name=name,detections=[dict(food=food(),confidence_score='high')],total_nutrients=NUTRIENTS,glucose_prediction=PREDICTION)
def log(name='Fixture breakfast'):
 f=food(); f.pop('servings');f.update(consumed_serving=dict(id=11,quantity=1),serving_details=dict(id=11,quantity=1,unit='cup',weight_grams=100))
 return dict(id='11111111-1111-4111-8111-111111111111',name=name,timestamp_utc='2026-08-31T12:00:00Z',foods=[f])
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
    return self.respond(dict(code='not_found',message='No v1.2 endpoint matches GET '+path+'. The API reference at /v1.2/docs lists every route.'),status)
   return self.respond(dict(code='fixture_error',message='The test request could not be completed.',request_id='parity-request',docs_url='https://example.invalid/fixture-docs'),status)
  if path.endswith('/autocomplete'):result=dict(items=[])
  elif path.endswith('/alternatives'):result=dict(alternatives=[] if empty else [dict(food=food(102,'Fixture lentils'))])
  elif path.endswith('/foods/101'):result=food()
  elif path.endswith('/foods/102'):result=food(102,'Fixture lentils')
  elif path.endswith('/foods') or '/foods/barcode/' in path:result=dict(total_count=0 if empty else 1,items=[] if empty else [food(full=False)])
  elif path.endswith('/restaurants/cafe/menu-items'):result=dict(total_count=0 if empty else 2,items=[] if empty or int(q.get('offset',0)) >= 2 else [dict(type='menu_item',id=str(101+int(q.get('offset',0))),name='Fixture bowl' if q.get('offset','0') == '0' else 'Fixture soup',restaurant_name='Fixture Cafe',image_url='',nutrients=NUTRIENTS,servings=SERVINGS)])
  elif path.endswith('/restaurants/menu-items'):
   restaurant_name='Fixture Cafe'
   result=dict(total_count=0 if empty else 2,items=[] if empty else [dict(type='menu_item',id='101',name='Fixture bowl',restaurant_name=restaurant_name,image_url='',nutrients=NUTRIENTS,servings=SERVINGS),dict(type='menu_item',id='102',name='Fixture soup',restaurant_name=restaurant_name,image_url='',nutrients=NUTRIENTS,servings=SERVINGS)])
  elif path.endswith('/restaurants'):result=dict(total_count=0 if empty else 1,items=[] if empty else [dict(type='restaurant',id='cafe',name='Fixture Cafe',city='San Francisco',address1='123 Test Street',is_chain=False)])
  elif path.endswith('/glucose/predictions'):result=PREDICTION
  elif path.endswith('/food-scans/photo'):result=scan()
  elif 'correct' in path:result=scan('Corrected breakfast')
  elif path.endswith('/food-scans/text'):result=dict(detections=[] if empty else [dict(food=food())],total_nutrients=NUTRIENTS)
  elif '/food-logs' in path:
   if self.command=='GET':result=dict(total_count=len(state['logs']),items=state['logs'])
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
