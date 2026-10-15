package com.ngo.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final JdbcTemplate db;
  public ApiController(JdbcTemplate db) { this.db = db; }

  record EntityDef(String table, String id, List<String> fields, String listSql, String orderBy) {}
  private final Map<String,EntityDef> defs = Map.ofEntries(
    Map.entry("programmes", new EntityDef("programme","programme_id", List.of("programme_name","description","start_date","end_date","status"), "SELECT programme_id AS id, programme_name AS name, description, start_date, end_date, status FROM programme", "programme_id")),
    Map.entry("campaigns", new EntityDef("campaign","campaign_id", List.of("programme_id","campaign_name","description","start_date","end_date","target_amount"), "SELECT c.campaign_id AS id,c.campaign_name AS name,p.programme_name AS programme,c.programme_id,c.description,c.start_date,c.end_date,c.target_amount FROM campaign c JOIN programme p ON p.programme_id=c.programme_id", "c.campaign_id")),
    Map.entry("donors", new EntityDef("donor","donor_id", List.of("donor_name","email","phone","donor_type"), "SELECT donor_id AS id,donor_name AS name,email,phone,donor_type FROM donor", "donor_id")),
    Map.entry("donations", new EntityDef("donation","donation_id", List.of("donor_id","campaign_id","donation_date","amount","payment_method"), "SELECT n.donation_id AS id,d.donor_name AS donor,c.campaign_name AS campaign,n.donor_id,n.campaign_id,n.donation_date,n.amount,n.payment_method FROM donation n JOIN donor d ON d.donor_id=n.donor_id JOIN campaign c ON c.campaign_id=n.campaign_id", "n.donation_date DESC,n.donation_id DESC")),
    Map.entry("beneficiaries", new EntityDef("beneficiary","beneficiary_id", List.of("name","contact","address","category"), "SELECT beneficiary_id AS id,name,contact,address,category FROM beneficiary", "beneficiary_id")),
    Map.entry("requests", new EntityDef("request","request_id", List.of("beneficiary_id","programme_id","request_date","description","requested_amount","status"), "SELECT r.request_id AS id,b.name AS beneficiary,p.programme_name AS programme,r.beneficiary_id,r.programme_id,r.request_date,r.description,r.requested_amount,r.status FROM request r JOIN beneficiary b ON b.beneficiary_id=r.beneficiary_id JOIN programme p ON p.programme_id=r.programme_id", "r.request_id DESC")),
    Map.entry("approvals", new EntityDef("approval","approval_id", List.of("request_id","approval_date","approved_amount","status"), "SELECT a.approval_id AS id,a.request_id,b.name AS beneficiary,r.description,a.approval_date,a.approved_amount,a.status FROM approval a JOIN request r ON r.request_id=a.request_id JOIN beneficiary b ON b.beneficiary_id=r.beneficiary_id", "a.approval_id DESC")),
    Map.entry("distributions", new EntityDef("distribution","distribution_id", List.of("request_id","distribution_date","amount_or_quantity","distribution_type"), "SELECT x.distribution_id AS id,x.request_id,b.name AS beneficiary,r.description,x.distribution_date,x.amount_or_quantity,x.distribution_type,r.status AS request_status FROM distribution x JOIN request r ON r.request_id=x.request_id JOIN beneficiary b ON b.beneficiary_id=r.beneficiary_id", "x.distribution_id DESC")),
    Map.entry("volunteers", new EntityDef("volunteer","volunteer_id", List.of("name","email","phone","availability"), "SELECT volunteer_id AS id,name,email,phone,availability FROM volunteer", "volunteer_id")),
    Map.entry("skills", new EntityDef("skill","skill_id", List.of("skill_name","description"), "SELECT skill_id AS id,skill_name AS name,description FROM skill", "skill_id")),
    Map.entry("assignments", new EntityDef("assignment","assignment_id", List.of("volunteer_id","skill_id","programme_id","role","assignment_date","end_date"), "SELECT a.assignment_id AS id,v.name AS volunteer,s.skill_name AS skill,p.programme_name AS programme,a.volunteer_id,a.skill_id,a.programme_id,a.role,a.assignment_date,a.end_date FROM assignment a JOIN volunteer v ON v.volunteer_id=a.volunteer_id JOIN skill s ON s.skill_id=a.skill_id JOIN programme p ON p.programme_id=a.programme_id", "a.assignment_id DESC")),
    Map.entry("expenses", new EntityDef("expense","expense_id", List.of("programme_id","campaign_id","expense_date","amount","description","category"), "SELECT e.expense_id AS id,p.programme_name AS programme,c.campaign_name AS campaign,e.programme_id,e.campaign_id,e.expense_date,e.amount,e.description,e.category FROM expense e JOIN programme p ON p.programme_id=e.programme_id LEFT JOIN campaign c ON c.campaign_id=e.campaign_id", "e.expense_date DESC,e.expense_id DESC")),
    Map.entry("impact", new EntityDef("impact_indicator","impact_id", List.of("programme_id","indicator_name","target_value","actual_value","measurement_date"), "SELECT i.impact_id AS id,p.programme_name AS programme,i.programme_id,i.indicator_name,i.target_value,i.actual_value,i.measurement_date,ROUND(i.actual_value/i.target_value*100,2) AS achievement_pct FROM impact_indicator i JOIN programme p ON p.programme_id=i.programme_id", "i.impact_id"))
  );

  @GetMapping("/health") public Map<String,Object> health(){ return Map.of("ok",true,"service","AEGIS NGO API"); }

  @GetMapping("/dashboard") public Map<String,Object> dashboard() {
    Map<String,Object> m=new LinkedHashMap<>();
    m.put("programmes", one("SELECT COUNT(*) FROM programme")); m.put("campaigns",one("SELECT COUNT(*) FROM campaign"));
    m.put("donors",one("SELECT COUNT(*) FROM donor")); m.put("donations",one("SELECT COUNT(*) FROM donation"));
    m.put("donationAmount",money("SELECT COALESCE(SUM(amount),0) FROM donation")); m.put("beneficiaries",one("SELECT COUNT(*) FROM beneficiary"));
    m.put("volunteers",one("SELECT COUNT(*) FROM volunteer")); m.put("expenses",money("SELECT COALESCE(SUM(amount),0) FROM expense"));
    m.put("pendingRequests",one("SELECT COUNT(*) FROM request WHERE status='Pending'")); m.put("aidDistributed",money("SELECT COALESCE(SUM(amount_or_quantity),0) FROM distribution"));
    m.put("monthlyDonations",db.queryForList("SELECT DATE_FORMAT(donation_date,'%b') month, COALESCE(SUM(amount),0) amount FROM donation GROUP BY YEAR(donation_date),MONTH(donation_date),DATE_FORMAT(donation_date,'%b') ORDER BY YEAR(donation_date),MONTH(donation_date)"));
    m.put("recentDonations",db.queryForList("SELECT n.donation_id id,d.donor_name donor,c.campaign_name campaign,n.amount,n.donation_date date,n.payment_method method FROM donation n JOIN donor d ON d.donor_id=n.donor_id JOIN campaign c ON c.campaign_id=n.campaign_id ORDER BY n.donation_id DESC LIMIT 6"));
    return m;
  }

  @GetMapping("/{entity}") public List<Map<String,Object>> list(@PathVariable String entity,@RequestParam(defaultValue="") String search){
    EntityDef e=require(entity); String sql=e.listSql();
    if(!search.isBlank()) sql += " WHERE " + searchable(e) + " LIKE ?";
    sql += " ORDER BY " + e.orderBy();
    return search.isBlank()?db.queryForList(sql):db.queryForList(sql,"%"+search.trim()+"%");
  }

  @GetMapping("/{entity}/{id}") public Map<String,Object> get(@PathVariable String entity,@PathVariable int id){
    EntityDef e=require(entity); return db.queryForMap("SELECT * FROM "+e.table()+" WHERE "+e.id()+"=?",id);
  }

  @PostMapping("/{entity}") @Transactional public Map<String,Object> create(@PathVariable String entity,@RequestBody Map<String,Object> body){
    if(entity.equals("donations")) return donation(body); if(entity.equals("approvals")) return approval(body); if(entity.equals("distributions")) return distribution(body); if(entity.equals("assignments")) return assignment(body);
    EntityDef e=require(entity); Map<String,Object> b=clean(e,body); String cols=String.join(",",b.keySet()); String qs=String.join(",",Collections.nCopies(b.size(),"?"));
    Number id=insertReturningId("INSERT INTO "+e.table()+" ("+cols+") VALUES ("+qs+")", b.values().toArray()); return get(entity,id.intValue());
  }

  @PutMapping("/{entity}/{id}") @Transactional public Map<String,Object> update(@PathVariable String entity,@PathVariable int id,@RequestBody Map<String,Object> body){
    if(entity.equals("approvals")||entity.equals("distributions")||entity.equals("donations")||entity.equals("assignments")) throw new IllegalArgumentException("This record is managed by a transaction workflow and cannot be edited here.");
    EntityDef e=require(entity); Map<String,Object>b=clean(e,body); String set=String.join(",",b.keySet().stream().map(k->k+"=?").toList()); Object[] args=new Object[b.size()+1]; int i=0; for(Object v:b.values())args[i++]=v;args[i]=id; db.update("UPDATE "+e.table()+" SET "+set+" WHERE "+e.id()+"=?",args); return get(entity,id);
  }

  @DeleteMapping("/{entity}/{id}") @Transactional public ResponseEntity<?> delete(@PathVariable String entity,@PathVariable int id){
    EntityDef e=require(entity); db.update("DELETE FROM "+e.table()+" WHERE "+e.id()+"=?",id); return ResponseEntity.noContent().build();
  }

  @GetMapping("/lookups") public Map<String,Object> lookups(){
    return Map.of("programmes",db.queryForList("SELECT programme_id id,programme_name name FROM programme ORDER BY programme_name"),"campaigns",db.queryForList("SELECT campaign_id id,campaign_name name FROM campaign ORDER BY campaign_name"),"donors",db.queryForList("SELECT donor_id id,donor_name name FROM donor ORDER BY donor_name"),"beneficiaries",db.queryForList("SELECT beneficiary_id id,name FROM beneficiary ORDER BY name"),"volunteers",db.queryForList("SELECT volunteer_id id,name FROM volunteer ORDER BY name"),"skills",db.queryForList("SELECT skill_id id,skill_name name FROM skill ORDER BY skill_name"),"pendingRequests",db.queryForList("SELECT request_id id,CONCAT('#',request_id,' ',description,' (',requested_amount,')') name FROM request WHERE status='Pending' ORDER BY request_id"),"approvedRequests",db.queryForList("SELECT request_id id,CONCAT('#',request_id,' ',description) name FROM request WHERE status='Approved' ORDER BY request_id"));
  }

  @GetMapping("/reports/{n}") public List<Map<String,Object>> report(@PathVariable int n){
    String[] q={"SELECT programme_name AS Programme,campaign_name AS Campaign,funds_received AS `Funds Received`,expenses AS Expenses,remaining_funds AS `Remaining Funds`,utilization_pct AS `Utilization %` FROM v_campaign_fund_utilization ORDER BY programme_id,campaign_id","SELECT donor_name AS Donor,donor_type AS Type,donation_count AS `No. of Donations`,total_contributed AS `Total Contributed`,campaigns_supported AS `Campaigns Supported` FROM v_donor_contribution_summary ORDER BY total_contributed DESC","SELECT beneficiary_name AS Beneficiary,category AS Category,total_requests AS Requests,approved_requests AS `Approved Requests`,approved_amount AS `Approved Amount`,distributed_amount AS Distributed,distribution_status AS `Distribution Status` FROM v_beneficiary_assistance_summary ORDER BY beneficiary_id","SELECT volunteer_name AS Volunteer,availability AS Availability,skills_used AS Skills,assignment_count AS Assignments,programmes_involved AS `Programmes Involved`,roles AS Roles FROM v_volunteer_activity_summary ORDER BY volunteer_id","SELECT programme_name AS Programme,category AS `Expense Category`,expense_count AS Entries,total_expenses AS `Total Expenses`,first_expense_date AS `From`,last_expense_date AS `To` FROM v_programme_expense_summary ORDER BY programme_id,category","SELECT programme_name AS Programme,indicator_name AS Indicator,target_value AS Target,actual_value AS Actual,achievement_pct AS `Achievement %`,measurement_date AS `Measured On` FROM v_programme_impact_summary ORDER BY programme_id,indicator_name","SELECT programme_name AS Programme,funds_received AS `Funds Received`,expenses AS Expenses,distributed_aid AS `Aid Distributed`,available_balance AS `Available Balance` FROM v_programme_fund_position ORDER BY programme_id"};
    if(n<1||n>7)throw new IllegalArgumentException("Report must be 1-7"); return db.queryForList(q[n-1]);
  }

  private Map<String,Object> donation(Map<String,Object>b){
    int donor=((Number)b.get("donor_id")).intValue(), campaign=((Number)b.get("campaign_id")).intValue(); Date date=Date.valueOf(String.valueOf(b.get("donation_date"))); BigDecimal amount=new BigDecimal(String.valueOf(b.get("amount"))); String method=String.valueOf(b.get("payment_method"));
    if(amount.signum()<=0)throw new IllegalArgumentException("Donation amount must be greater than zero.");
    Map<String,Object> c=db.queryForMap("SELECT start_date,end_date FROM campaign WHERE campaign_id=?",campaign); if(date.before((Date)c.get("start_date")) || (c.get("end_date")!=null&&date.after((Date)c.get("end_date"))))throw new IllegalArgumentException("Donation date is outside the campaign dates.");
    Number id=insertReturningId("INSERT INTO donation(donor_id,campaign_id,donation_date,amount,payment_method) VALUES(?,?,?,?,?)",donor,campaign,date,amount,method); return get("donations",id.intValue());
  }
  private Map<String,Object> approval(Map<String,Object>b){ int rid=((Number)b.get("request_id")).intValue(); String status=String.valueOf(b.get("status")); BigDecimal amount=new BigDecimal(String.valueOf(b.getOrDefault("approved_amount","0"))); Date date=Date.valueOf(String.valueOf(b.get("approval_date"))); Map<String,Object> r=db.queryForMap("SELECT status,requested_amount FROM request WHERE request_id=? FOR UPDATE",rid); if(!"Pending".equals(r.get("status")))throw new IllegalArgumentException("Only Pending requests can be decided."); if("Approved".equals(status)&&amount.signum()<=0)throw new IllegalArgumentException("Approved amount must be greater than zero."); if(amount.compareTo((BigDecimal)r.get("requested_amount"))>0)throw new IllegalArgumentException("Approved amount cannot exceed requested amount."); if("Rejected".equals(status))amount=BigDecimal.ZERO; Number id=insertReturningId("INSERT INTO approval(request_id,approval_date,approved_amount,status) VALUES(?,?,?,?)",rid,date,amount,status); return get("approvals",id.intValue()); }
  private Map<String,Object> distribution(Map<String,Object>b){ int rid=((Number)b.get("request_id")).intValue(); BigDecimal amount=new BigDecimal(String.valueOf(b.get("amount_or_quantity"))); if(amount.signum()<=0)throw new IllegalArgumentException("Distribution amount must be greater than zero."); Map<String,Object> a=db.queryForMap("SELECT a.approved_amount,(SELECT COALESCE(SUM(amount_or_quantity),0) FROM distribution WHERE request_id=?) done FROM approval a WHERE a.request_id=? AND a.status='Approved'",rid,rid); BigDecimal remain=((BigDecimal)a.get("approved_amount")).subtract((BigDecimal)a.get("done")); if(amount.compareTo(remain)>0)throw new IllegalArgumentException("Distribution exceeds remaining approved amount: "+remain); Number id=insertReturningId("INSERT INTO distribution(request_id,distribution_date,amount_or_quantity,distribution_type) VALUES(?,?,?,?)",rid,Date.valueOf(String.valueOf(b.get("distribution_date"))),amount,String.valueOf(b.get("distribution_type"))); return get("distributions",id.intValue()); }
  private Map<String,Object> assignment(Map<String,Object>b){ Number id=insertReturningId("INSERT INTO assignment(volunteer_id,skill_id,programme_id,role,assignment_date,end_date) VALUES(?,?,?,?,?,?)",((Number)b.get("volunteer_id")).intValue(),((Number)b.get("skill_id")).intValue(),((Number)b.get("programme_id")).intValue(),b.get("role"),Date.valueOf(String.valueOf(b.get("assignment_date"))),Date.valueOf(String.valueOf(b.get("end_date")))); return get("assignments",id.intValue()); }
  private EntityDef require(String k){EntityDef e=defs.get(k);if(e==null)throw new IllegalArgumentException("Unknown entity: "+k);return e;}
  private Map<String,Object> clean(EntityDef e,Map<String,Object>b){Map<String,Object>m=new LinkedHashMap<>();for(String f:e.fields())if(b.containsKey(f)&&b.get(f)!=null&&!String.valueOf(b.get(f)).isBlank()){Object v=b.get(f);if(f.endsWith("_date")||f.equals("start_date")||f.equals("end_date")||f.equals("measurement_date"))v=Date.valueOf(String.valueOf(v)); if(f.contains("amount")||f.equals("target_amount"))v=new BigDecimal(String.valueOf(v));m.put(f,v);}return m;}
  private String searchable(EntityDef e){return "CONCAT_WS(' ',"+String.join(",",e.fields())+")";}
  private Number insertReturningId(String sql,Object... args){KeyHolder kh=new GeneratedKeyHolder(); db.update(c->{var ps=c.prepareStatement(sql,java.sql.Statement.RETURN_GENERATED_KEYS); for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]); return ps;},kh); return kh.getKey();}
  private long one(String sql){return db.queryForObject(sql,Long.class);}
  private String money(String sql){return String.format("%,.0f",db.queryForObject(sql,BigDecimal.class));}

  @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,Object>> error(Exception e){Throwable t=e;while(t.getCause()!=null&&!(t instanceof DataIntegrityViolationException))t=t.getCause();return ResponseEntity.badRequest().body(Map.of("error",Optional.ofNullable(e.getMessage()).orElse("Request failed")));}
}
