"""Local-only API fixtures shared by the real Android and iOS demo workflows.
No credentials and no proxying to production. Control routes are test-only.
"""
import json, time, threading
from datetime import datetime, timedelta, timezone
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from pathlib import Path

NUTRIENTS = {k: {'value':v,'unit':u} for k,v,u in [('calories',100,'kcal'),('protein',4,'g'),('carbohydrates',20,'g'),('total_fat',2,'g'),('fiber',3,'g'),('sodium',10,'mg')]}
SERVINGS = [dict(id='11',quantity=1,unit='cup',scaling_factor=1,weight_grams=100,is_primary=True),dict(id='12',quantity=1,unit='oz',scaling_factor=0.2835,weight_grams=28.35,is_primary=False)]
def food(id='101',name='Fixture oatmeal',full=True):return dict(id=str(id),type='generic',name=name,brand_name='January fixture',nutrients=NUTRIENTS,glycemic_index=52,glycemic_load=12,image_url=None,barcode=None,servings=SERVINGS if full else SERVINGS[:1])
PREDICTION = dict(points=[dict(minutes=m,value=v) for m,v in [(0,90),(30,125),(60,140),(90,115),(120,95)]],impact_score='medium',chart=dict(min=70,max=140))
def suggestion(id,name):return dict(id=str(id),type='generic',name=name,brand_name=None,image_url=None,nutrients=NUTRIENTS)
def detected(id='101',name='Fixture oatmeal'):return dict(id=str(id),name=name,brand_name='January fixture',nutrients=NUTRIENTS,quantity=1,serving=dict(id='11',quantity=1,unit='cup'))
def scan(name='Fixture breakfast'):return dict(meal_name=name,detections=[dict(food=detected(),confidence='high')],total_nutrients=NUTRIENTS)
# The end user's timezone in the demos' fixture launches (FixtureLaunch.kt on Android).
DEMO_TIMEZONE='America/New_York'
def seeded_eaten_at():
 # An hour ago, so the seeded log is in the past, but never before today's first minute in the demo's
 # timezone: between midnight and 1 AM an hour ago is yesterday, and flows expect the log today.
 now=datetime.now(timezone.utc)
 start_of_today=datetime.now(__import__('zoneinfo').ZoneInfo(DEMO_TIMEZONE)).replace(hour=0,minute=1,second=0,microsecond=0)
 return max(now-timedelta(hours=1),start_of_today.astimezone(timezone.utc)).strftime('%Y-%m-%dT%H:%M:%SZ')
def local_day(ts,q):
 # The calendar day of a UTC timestamp in the request's timezone, like the API.
 try:zone=__import__('zoneinfo').ZoneInfo(q.get('timezone','UTC'))
 except Exception:zone=timezone.utc
 return datetime.fromisoformat(ts.replace('Z','+00:00')).astimezone(zone).strftime('%Y-%m-%d')
def in_range(day,q):return (not q.get('start_date') or day>=q['start_date']) and (not q.get('end_date') or day<=q['end_date'])
def day_range(q):
 start=q.get('start_date');end=q.get('end_date',start)
 if not start:return []
 first=datetime.strptime(start,'%Y-%m-%d');last=datetime.strptime(end,'%Y-%m-%d');days=[]
 while first<=last and len(days)<366:days.append(first.strftime('%Y-%m-%d'));first+=timedelta(days=1)
 return days
def add_nutrients(total,extra):
 for k,v in extra.items():
  if k in total:total[k]=dict(value=round(total[k]['value']+v['value'],4),unit=v['unit'])
  else:total[k]=dict(v)
 return total
def summary(q):
 # One bucket per day, like group_by=day; every day in the range is present.
 buckets=[];totals={};logs_count=0;days_with_logs=0
 for day in day_range(q):
  day_logs=[l for l in state['logs'] if local_day(l['eaten_at'],q)==day];nutrients={}
  for l in day_logs:
   for f in l['foods']:add_nutrients(nutrients,f['nutrients'])
  buckets.append(dict(start_date=day,end_date=day,logs_count=len(day_logs),days_with_logs=1 if day_logs else 0,nutrients=nutrients))
  logs_count+=len(day_logs);days_with_logs+=1 if day_logs else 0;add_nutrients(totals,nutrients)
 average={k:dict(value=round(v['value']/days_with_logs,4),unit=v['unit']) for k,v in totals.items()} if days_with_logs else {}
 return dict(group_by='day',week_start=None,timezone=q.get('timezone','UTC'),start_date=q.get('start_date'),end_date=q.get('end_date',q.get('start_date')),buckets=buckets,totals=dict(logs_count=logs_count,days_with_logs=days_with_logs,nutrients=totals),average_per_logged_day=dict(nutrients=average))
def log(name='Fixture breakfast'):
 f=food(); f.pop('servings');f.pop('type');f.pop('barcode');f.update(food_id=f.pop('id'),quantity=1,serving=dict(id='11',quantity=1,unit='cup',weight_grams=100))
 return dict(id='11111111-1111-4111-8111-111111111111',name=name,eaten_at=seeded_eaten_at(),foods=[f])
ML_PER_FL_OZ=29.5735
ML_PER_UNIT=dict(fl_oz=ML_PER_FL_OZ,cup=8*ML_PER_FL_OZ,ml=1)
def volume(ml,unit):return dict(value=round(ml/ML_PER_UNIT.get(unit,1),1),unit=unit)
def stamp(value):
 # Any ISO-8601 offset in, UTC with milliseconds out, like the API.
 if value:
  parsed=datetime.fromisoformat(value.replace('Z','+00:00'))
 else:parsed=datetime.now(timezone.utc)
 return parsed.astimezone(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.')+'%03dZ'%(parsed.microsecond//1000)
def history():
 # About 13 months of water and weight ending today at local noon on this host, which shares
 # the device's timezone (a UTC date would be tomorrow's on an American evening): water on
 # three days in four, a weight every third day drifting down toward today, every fourth
 # weight logged in pounds.
 today=datetime.now().astimezone().replace(hour=12,minute=0,second=0,microsecond=0).astimezone(timezone.utc);water=[];weights=[]
 for i in range(400):
  at=(today-timedelta(days=i)).strftime('%Y-%m-%dT%H:%M:%S.000Z')
  if i%4!=3:water.append(dict(id='history-%d'%i,ml=1400+(i*137)%900,consumed_at=at))
  if i%3==0:
   kg=round(70+i*0.012+((i*7)%5)*0.1,1)
   weights.append(dict(weight=dict(value=round(kg/0.45359237,1),unit='lb') if i%12==0 else dict(value=kg,unit='kg'),measured_at=at))
 return water,weights
LIST_LIMIT=100
state={'rules':{},'logs':[],'water':[],'weights':[],'requests':[]}
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
  if path=='/__reset':state.update(rules={},logs=[],water=[],weights=[],requests=[]);return self.respond({})
  if path=='/__control':state['rules'][q['route']]=q;return self.respond({})
  if path=='/__seed':state['logs']=[log()];return self.respond({})
  if path=='/__seed_history':state['water'],state['weights']=history();return self.respond({})
  if path=='/__requests':return self.respond(state['requests'])
  state['requests'].append(dict(method=self.command,path=path,query=q,body=body,auth=self.headers.get('Authorization'),end_user=self.headers.get('January-End-User-ID')))
  if path=='/api/january/client-token':
   # Stands in for a token relay: the token names the end user it was minted for.
   user=self.headers.get('January-End-User-ID','')
   if not user:return self.respond(dict(code='invalid_request',message='January-End-User-ID is required.'),400)
   return self.respond(dict(token='fixture-token-'+user,expires_in=3600))
  rule=state['rules'].get(path,{})
  delay=float(rule.get('delay',0))
  if delay:time.sleep(delay)
  status=int(rule.get('status',200));empty=rule.get('empty')=='true'
  if status!=200:
   if status==404 and '/restaurants/' in path and path.endswith('/menu-items'):
    restaurant_id=path.split('/restaurants/',1)[1].split('/menu-items',1)[0]
    return self.respond(dict(code='not_found',message='No restaurant with id '+restaurant_id+'. Use an id from a GET /v1.2/restaurants result.'),status)
   return self.respond(dict(code='fixture_error',message='The test request could not be completed.',request_id='parity-request',docs_url='https://example.invalid/fixture-docs'),status)
  if path.endswith('/autocomplete'):
   # Suggestions only for "ban…" (as in the API's "ban" → banana example), so no other flow's typing opens the list.
   result=dict(items=[] if empty or not q.get('query','').lower().startswith('ban') else [suggestion('101','banana'),suggestion('102','banana bread')])
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
  elif path.endswith('/food-analysis/text'):result=dict(meal_name=None,detections=[] if empty else [dict(food=detected(),confidence=None)],total_nutrients=NUTRIENTS)
  elif '/water-logs' in path:
   if self.command=='POST':
    amount=body.get('amount',{});ml=float(amount.get('value',0))*ML_PER_UNIT.get(amount.get('unit'),1)
    result=dict(id=str(__import__('uuid').uuid4()),amount=dict(value=amount.get('value'),unit=amount.get('unit')),consumed_at=stamp(body.get('consumed_at')))
    state['water'].append(dict(id=result['id'],ml=ml,consumed_at=result['consumed_at']));return self.respond(result,201)
   if self.command=='DELETE':
    state['water']=[w for w in state['water'] if w['id']!=path.rsplit('/',1)[1]];return self.respond(None,204)
   totals={}
   for w in state['water']:day=local_day(w['consumed_at'],q);totals[day]=totals.get(day,0)+w['ml']
   result=dict(items=[] if empty else [dict(date=d,total=volume(ml,q.get('unit','fl_oz'))) for d,ml in sorted(totals.items()) if in_range(d,q)][-LIST_LIMIT:])
  elif '/weight-logs' in path:
   if self.command=='POST':
    result=dict(weight=body.get('weight'),measured_at=stamp(body.get('measured_at')))
    state['weights'].append(result);return self.respond(result,201)
   latest={}
   for w in state['weights']:
    day=local_day(w['measured_at'],q)
    if day not in latest or w['measured_at']>=latest[day]['measured_at']:latest[day]=w
   result=dict(items=[] if empty else [dict(date=d,weight=w['weight']) for d,w in sorted(latest.items()) if in_range(d,q)][-LIST_LIMIT:])
  elif path.endswith('/food-logs/summary'):result=summary(q)
  elif '/food-logs' in path:
   if self.command=='GET':result=dict(items=[] if empty else [l for l in state['logs'] if in_range(local_day(l['eaten_at'],q),q)])
   elif self.command=='DELETE':state['logs']=[];result=dict(status='success')
   else:
    result=log(body.get('name') or 'Fixture breakfast')
    if body.get('eaten_at'):result['eaten_at']=stamp(body['eaten_at'])
    state['logs']=[result]
  else:return self.respond(dict(message='Unmapped fixture route '+path),404)
  self.respond(result)
 def respond(self,body,status=200):
  if status==204:self.send_response(status);self.send_header('Content-Length','0');self.end_headers();return
  data=json.dumps(body).encode();self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(data)));self.end_headers()
  try:self.wfile.write(data)
  except (BrokenPipeError,ConnectionResetError):pass
if __name__=='__main__':ThreadingHTTPServer(('127.0.0.1',int(__import__('sys').argv[1]) if len(__import__('sys').argv)>1 else 18765),Handler).serve_forever()
