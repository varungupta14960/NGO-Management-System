-- sample_data.sql : realistic data. Insert order respects FKs AND business-rule triggers
-- (donations before distributions; requests -> approvals -> distributions).
USE ngo_db;

INSERT INTO programme (programme_id, programme_name, description, start_date, end_date, status) VALUES
(1,'Rural Education Support','School fees, kits and digital classrooms for village children','2025-04-01','2027-03-31','Active'),
(2,'Community Health Outreach','Mobile health camps, surgeries and child nutrition','2025-06-01','2027-05-31','Active'),
(3,'Food Security Initiative','Ration packs and community kitchens for hungry families','2025-01-15','2026-12-31','Active'),
(4,'Women Empowerment & Livelihood','Tailoring centres and micro-enterprise seed funds','2025-08-01','2027-07-31','Active'),
(5,'Disaster Relief Fund','Emergency relief during floods and cyclones','2025-09-01',NULL,'Active'),
(6,'Clean Water Access','Borewells, hand pumps and water storage for villages','2025-03-01','2026-12-31','Active'),
(7,'Elderly Care Services','Winter care and health check-ups for senior citizens','2024-01-01','2025-12-31','Completed'),
(8,'Youth Skill Development','Coding and vocational bootcamps for youth','2026-11-01','2028-10-31','Planned');

INSERT INTO campaign (campaign_id, programme_id, campaign_name, description, start_date, end_date, target_amount) VALUES
(1,1,'Back to School 2025','School kits and fee support','2025-05-01','2025-09-30',500000),
(2,1,'Digital Classrooms','Smart boards and tablets for schools','2026-01-01','2026-12-31',800000),
(3,2,'Mobile Health Camps','Free medical camps in remote villages','2025-07-01','2026-06-30',600000),
(4,2,'Child Nutrition Drive','Supplements for malnourished children','2026-02-01','2026-12-31',400000),
(5,3,'Monsoon Food Packs','Ration for flood-prone families','2025-06-01','2025-10-31',450000),
(6,3,'Community Kitchen','Daily hot meals in urban slums','2026-01-01','2026-12-31',700000),
(7,4,'Women Tailoring Centres','Sewing machines and trainers','2025-09-01','2026-08-31',550000),
(8,4,'Micro-Enterprise Seed Fund','Seed capital for women entrepreneurs','2026-03-01','2027-03-31',650000),
(9,5,'Flood Relief Appeal','Relief for flood-affected families','2025-10-01','2026-03-31',1200000),
(10,5,'Cyclone Emergency Kits','Pre-positioned cyclone kits','2026-05-01',NULL,900000),
(11,6,'Village Borewell Project','Borewells and pipelines','2025-04-01','2026-12-31',1000000),
(12,7,'Elders Winter Care','Blankets and check-ups for elders','2024-10-01','2025-03-31',250000),
(13,8,'Youth Coding Bootcamp','Laptops and mentors for a coding bootcamp','2026-11-01','2027-06-30',300000);

INSERT INTO donor (donor_id, donor_name, email, phone, donor_type) VALUES
(1,'Ananya Sharma','ananya.sharma@gmail.com','9876501001','Individual'),
(2,'Rajesh Kumar','rajesh.kumar@outlook.com','9876501002','Individual'),
(3,'Priya Nair','priya.nair@gmail.com','9876501003','Individual'),
(4,'Mohammed Faizan','faizan.m@yahoo.com','9876501004','Individual'),
(5,'Sunita Reddy','sunita.reddy@gmail.com','9876501005','Individual'),
(6,'Vikram Singh','vikram.singh@gmail.com','9876501006','Individual'),
(7,'Sunrise Technologies Pvt Ltd','csr@sunrisetech.com','9876501007','Corporate'),
(8,'GreenLeaf Foods Ltd','csr@greenleaffoods.com','9876501008','Corporate'),
(9,'Bharat Care Foundation','grants@bharatcare.org','9876501009','Foundation'),
(10,'Hope Trust','info@hopetrust.org','9876501010','Foundation'),
(11,'State Social Welfare Department','welfare@state.gov.in','9876501011','Government'),
(12,'Kavita Joshi','kavita.joshi@gmail.com','9876501012','Individual'),
(13,'Arjun Mehta','arjun.mehta@gmail.com','9876501013','Individual'),
(14,'Deepa Iyer','deepa.iyer@gmail.com','9876501014','Individual'),
(15,'Orbit Infra Corp','csr@orbitinfra.com','9876501015','Corporate'),
(16,'Sanjay Patel','sanjay.patel@gmail.com','9876501016','Individual'),
(17,'Lakshmi Narayanan','lakshmi.n@gmail.com','9876501017','Individual'),
(18,'Rotary Club Hyderabad','office@rotaryhyd.org','9876501018','Foundation');

INSERT INTO donation (donation_id, donor_id, campaign_id, donation_date, amount, payment_method) VALUES
(1,1,1,'2025-05-10',25000,'UPI'),(2,7,1,'2025-06-02',150000,'Bank Transfer'),(3,2,1,'2025-07-15',10000,'Cash'),
(4,9,2,'2026-01-20',200000,'Bank Transfer'),(5,3,2,'2026-03-05',15000,'UPI'),(6,7,2,'2026-05-11',100000,'Bank Transfer'),
(7,10,3,'2025-07-20',120000,'Bank Transfer'),(8,4,3,'2025-09-09',5000,'UPI'),(9,11,3,'2025-11-01',200000,'Bank Transfer'),
(10,5,4,'2026-02-14',12000,'Card'),(11,8,4,'2026-04-01',80000,'Bank Transfer'),(12,12,4,'2026-06-18',7500,'UPI'),
(13,8,5,'2025-06-15',100000,'Bank Transfer'),(14,6,5,'2025-08-01',20000,'Cheque'),(15,18,5,'2025-09-12',60000,'Cheque'),
(16,13,6,'2026-01-25',30000,'UPI'),(17,8,6,'2026-03-10',150000,'Bank Transfer'),(18,1,6,'2026-07-04',15000,'UPI'),
(19,9,7,'2025-09-25',180000,'Bank Transfer'),(20,14,7,'2025-11-11',9000,'UPI'),(21,15,7,'2026-01-30',90000,'Bank Transfer'),
(22,3,8,'2026-03-20',25000,'UPI'),(23,10,8,'2026-05-05',100000,'Bank Transfer'),
(24,11,9,'2025-10-10',300000,'Bank Transfer'),(25,7,9,'2025-10-15',250000,'Bank Transfer'),(26,16,9,'2025-11-20',20000,'UPI'),
(27,2,9,'2026-01-08',5000,'Cash'),(28,15,10,'2026-05-20',120000,'Bank Transfer'),(29,4,10,'2026-08-02',3000,'UPI'),
(30,18,11,'2025-04-20',150000,'Cheque'),(31,9,11,'2025-06-30',220000,'Bank Transfer'),(32,6,11,'2025-12-12',30000,'Cheque'),
(33,15,11,'2026-02-02',90000,'Bank Transfer'),
(34,10,12,'2024-11-10',80000,'Bank Transfer'),(35,14,12,'2024-12-20',12000,'UPI'),(36,5,12,'2025-01-18',6000,'Cash');

INSERT INTO beneficiary (beneficiary_id, name, contact, address, category) VALUES
(1,'Sita Devi','9123400001','Ward 4, Nandigama Village, Krishna District','Women'),
(2,'Ramesh Yadav','9123400002','Shanti Nagar Slum, Hyderabad','Family'),
(3,'Anita Kumari','9123400003','Gram Panchayat Road, Medak','Student'),
(4,'Mohan Lal','9123400004','Old Town, Warangal','Elderly'),
(5,'Fatima Begum','9123400005','Charminar Lane 7, Hyderabad','Women'),
(6,'Suresh Babu','9123400006','Kondapur Village, Sangareddy','Disabled'),
(7,'Pooja Kumari','9123400007','Mandal Colony, Nizamabad','Child'),
(8,'Gopal Das','9123400008','Station Road, Karimnagar','Elderly'),
(9,'Meena Patel','9123400009','Gandhi Nagar, Secunderabad','Women'),
(10,'Rahul Verma','9123400010','Hostel Lane, Adilabad','Student'),
(11,'Kamala Bai','9123400011','Tank Bund Colony, Khammam','Elderly'),
(12,'Imran Sheikh','9123400012','Bastis of Yakutpura, Hyderabad','Family'),
(13,'Radha Krishnan','9123400013','Temple Street, Nalgonda','Disabled'),
(14,'Salma Khatun','9123400014','Block C, Mehdipatnam, Hyderabad','Child'),
(15,'Dinesh Rao','9123400015','Coastal Road, Machilipatnam','Family'),
(16,'Geeta Kumari','9123400016','Village Rampur, Mahabubnagar','Student');

INSERT INTO skill (skill_id, skill_name, description) VALUES
(1,'Teaching','Classroom teaching and tutoring'),(2,'First Aid','Basic first aid and emergency response'),
(3,'Nursing','Nursing and medical assistance'),(4,'Cooking','Large-scale cooking and catering'),
(5,'Logistics','Transport, packing and supply management'),(6,'Fundraising','Donor outreach and fundraising'),
(7,'Counselling','Psychological and social counselling'),(8,'IT Support','Computers, data entry and networks'),
(9,'Event Management','Organising camps and events'),(10,'Plumbing','Water pumps, pipes and borewells');

INSERT INTO volunteer (volunteer_id, name, email, phone, availability) VALUES
(1,'Neha Gupta','neha.gupta@gmail.com','9988700001','Weekdays'),
(2,'Karthik Rao','karthik.rao@gmail.com','9988700002','Full-time'),
(3,'Dr. Swathi Menon','swathi.menon@gmail.com','9988700003','Weekends'),
(4,'Imtiaz Ali','imtiaz.ali@gmail.com','9988700004','Evenings'),
(5,'Rukmini Devi','rukmini.devi@gmail.com','9988700005','Full-time'),
(6,'Harish Chandra','harish.c@gmail.com','9988700006','Flexible'),
(7,'Pallavi Deshmukh','pallavi.d@gmail.com','9988700007','Weekdays'),
(8,'Sameer Khan','sameer.khan@gmail.com','9988700008','Weekends'),
(9,'Divya Prakash','divya.prakash@gmail.com','9988700009','Flexible'),
(10,'Naveen Kumar','naveen.kumar@gmail.com','9988700010','Full-time'),
(11,'Dr. Anil Kapoor','anil.kapoor@gmail.com','9988700011','Weekends'),
(12,'Yusuf Pathan','yusuf.pathan@gmail.com','9988700012','Weekdays'),
(13,'Shalini Bose','shalini.bose@gmail.com','9988700013','Evenings'),
(14,'Tarun Malhotra','tarun.malhotra@gmail.com','9988700014','Flexible'),
(15,'Bhavna Shah','bhavna.shah@gmail.com','9988700015','Weekends'),
(16,'Jayanth Reddy','jayanth.reddy@gmail.com','9988700016','Evenings');

INSERT INTO request (request_id, beneficiary_id, programme_id, request_date, description, requested_amount) VALUES
(1,3,1,'2025-06-10','School fees and books',15000),
(2,10,1,'2025-07-05','Laptop for online classes',30000),
(3,16,1,'2026-02-10','Uniform and stationery',8000),
(4,7,1,'2026-04-18','Tuition support',12000),
(5,1,2,'2025-08-12','Surgery support',60000),
(6,4,2,'2025-09-22','Cataract operation',25000),
(7,6,2,'2026-01-14','Wheelchair',18000),
(8,14,2,'2026-03-03','Nutrition supplements for child',6000),
(9,11,2,'2026-06-30','Hearing aid',14000),
(10,2,3,'2025-07-01','Monsoon ration for family',10000),
(11,12,3,'2025-08-20','Food packs',9000),
(12,8,3,'2026-02-05','Daily meals for elderly',20000),
(13,15,3,'2026-08-01','Ration support',7000),
(14,5,4,'2025-10-05','Sewing machine',12000),
(15,9,4,'2026-03-25','Seed capital for tailoring shop',40000),
(16,1,4,'2026-05-12','Skill training fees',10000),
(17,13,4,'2026-09-10','Assistive tools for work',15000),
(18,2,5,'2025-10-20','Flood damaged home repair',50000),
(19,12,5,'2025-11-02','Relief kit',5000),
(20,15,5,'2026-05-25','Cyclone kit and shelter',20000),
(21,6,6,'2025-06-12','Hand pump installation near home',35000),
(22,11,6,'2026-08-14','Water storage tank',22000);

-- Decisions (trigger trg_approval_ai moves REQUEST.status to Approved / Rejected)
INSERT INTO approval (approval_id, request_id, approval_date, approved_amount, status) VALUES
(1,1,'2025-06-15',15000,'Approved'),(2,2,'2025-07-10',25000,'Approved'),(3,3,'2026-02-15',8000,'Approved'),
(4,4,'2026-04-25',0,'Rejected'),(5,5,'2025-08-20',50000,'Approved'),(6,6,'2025-09-28',25000,'Approved'),
(7,7,'2026-01-20',18000,'Approved'),(8,8,'2026-03-08',6000,'Approved'),(9,10,'2025-07-05',10000,'Approved'),
(10,11,'2025-08-25',9000,'Approved'),(11,12,'2026-02-10',15000,'Approved'),(12,14,'2025-10-10',12000,'Approved'),
(13,15,'2026-04-01',35000,'Approved'),(14,16,'2026-05-20',0,'Rejected'),(15,18,'2025-10-25',45000,'Approved'),
(16,19,'2025-11-05',5000,'Approved'),(17,20,'2026-05-30',20000,'Approved'),(18,21,'2025-06-18',35000,'Approved');

-- Distributions (trigger moves fully distributed requests to Fulfilled)
INSERT INTO distribution (distribution_id, request_id, distribution_date, amount_or_quantity, distribution_type) VALUES
(1,1,'2025-06-20',15000,'Cash'),(2,2,'2025-07-18',25000,'Equipment'),(3,3,'2026-02-25',5000,'Clothing'),
(4,5,'2025-09-01',30000,'Medical'),(5,5,'2025-10-10',20000,'Medical'),(6,6,'2025-10-05',25000,'Medical'),
(7,8,'2026-03-12',6000,'Food'),(8,10,'2025-07-10',10000,'Food'),(9,11,'2025-08-30',9000,'Food'),
(10,12,'2026-02-20',8000,'Food'),(11,14,'2025-10-15',12000,'Equipment'),(12,15,'2026-04-10',20000,'Cash'),
(13,18,'2025-11-01',45000,'Shelter'),(14,19,'2025-11-08',5000,'Shelter'),(15,20,'2026-06-05',10000,'Shelter'),
(16,21,'2025-06-25',35000,'Equipment');

UPDATE request SET status = 'Cancelled' WHERE request_id = 22;   -- Pending -> Cancelled (allowed)

INSERT INTO assignment (assignment_id, volunteer_id, skill_id, programme_id, assignment_date, end_date, role) VALUES
(1,1,1,1,'2025-05-01','2025-09-30','Lead Teacher'),(2,1,1,1,'2026-01-10','2026-06-30','Lead Teacher'),
(3,2,8,1,'2026-01-15','2026-12-31','IT Coordinator'),(4,3,3,2,'2025-07-01','2026-06-30','Camp Nurse'),
(5,4,2,2,'2025-07-01','2025-12-31','First Aid Volunteer'),(6,4,2,2,'2026-02-01','2026-08-31','First Aid Volunteer'),
(7,5,4,3,'2025-06-01','2025-10-31','Kitchen Lead'),(8,5,4,3,'2026-01-05','2026-12-31','Kitchen Lead'),
(9,6,5,3,'2025-06-15','2026-03-31','Logistics Coordinator'),(10,7,7,2,'2025-08-01','2026-01-31','Counsellor'),
(11,8,6,4,'2025-09-01','2026-02-28','Fundraising Officer'),(12,9,1,4,'2025-09-15','2026-08-31','Skills Trainer'),
(13,10,9,5,'2025-10-01','2025-12-31','Event Coordinator'),(14,10,5,5,'2026-05-01','2026-09-30','Relief Logistics Lead'),
(15,11,3,5,'2025-10-05','2025-11-30','Medical Responder'),(16,12,10,6,'2025-04-01','2025-12-31','Water Systems Technician'),
(17,12,10,6,'2026-01-15','2026-12-31','Water Systems Technician'),(18,13,7,4,'2026-03-01','2026-12-31','Counsellor'),
(19,14,8,6,'2025-05-01','2025-10-31','Data Volunteer'),(20,15,2,5,'2026-05-10','2026-08-31','First Aid Volunteer'),
(21,15,2,7,'2024-10-15','2025-03-15','Care Volunteer'),(22,3,7,7,'2024-11-01','2025-03-01','Elder Care Counsellor'),
(23,9,9,2,'2026-09-01','2026-12-15','Camp Event Organiser');

INSERT INTO expense (expense_id, programme_id, campaign_id, expense_date, amount, description, category) VALUES
(1,1,1,'2025-06-05',42000,'Notebooks and school kits','Supplies'),
(2,1,1,'2025-08-12',18500,'Transport of school kits','Logistics'),
(3,1,2,'2026-02-10',95000,'Smart board purchase','Education'),
(4,1,NULL,'2026-04-01',22000,'Programme coordination','Administration'),
(5,2,3,'2025-08-05',70000,'Medicines for health camps','Medical'),
(6,2,3,'2025-10-18',25000,'Mobile van fuel and maintenance','Logistics'),
(7,2,4,'2026-03-12',38000,'Nutrition packs','Food'),
(8,2,NULL,'2026-05-02',60000,'Medical staff stipends','Salaries'),
(9,3,5,'2025-07-14',85000,'Bulk grain and oil procurement','Food'),
(10,3,5,'2025-09-20',14000,'Packing and delivery','Logistics'),
(11,3,6,'2026-02-15',120000,'Community kitchen operations','Food'),
(12,3,NULL,'2026-06-10',18000,'Audit and admin','Administration'),
(13,4,7,'2025-10-10',110000,'Sewing machines and material','Supplies'),
(14,4,7,'2025-12-05',36000,'Trainer honorarium','Salaries'),
(15,4,8,'2026-04-20',15000,'Entrepreneurship workshop','Events'),
(16,5,9,'2025-10-18',140000,'Relief material transport','Logistics'),
(17,5,9,'2025-11-10',180000,'Tarpaulins and blankets','Supplies'),
(18,5,10,'2026-06-01',65000,'Emergency kits','Supplies'),
(19,5,NULL,'2026-07-15',12000,'Relief coordination','Administration'),
(20,6,11,'2025-05-10',220000,'Borewell drilling','Construction'),
(21,6,11,'2025-09-14',150000,'Pump and pipeline','Construction'),
(22,6,11,'2026-03-03',45000,'Water testing kits','Supplies'),
(23,7,12,'2024-11-20',40000,'Blankets and warm clothing','Supplies'),
(24,7,12,'2025-01-25',22000,'Health check-ups','Medical'),
(25,7,NULL,'2025-03-10',8000,'Closing event','Events'),
(26,5,9,'2026-01-20',9000,'Donor transparency meet','Events');

INSERT INTO impact_indicator (impact_id, programme_id, indicator_name, target_value, actual_value, measurement_date) VALUES
(1,1,'Students supported',500,430,'2026-09-30'),(2,1,'Digital classrooms set up',20,12,'2026-09-30'),
(3,1,'Attendance improvement (%)',15,11,'2026-09-30'),(4,2,'Patients treated',3000,2650,'2026-09-30'),
(5,2,'Health camps held',40,36,'2026-09-30'),(6,2,'Children covered by nutrition support',800,520,'2026-09-30'),
(7,3,'Meals served',60000,48500,'2026-09-30'),(8,3,'Families supported',400,380,'2026-09-30'),
(9,4,'Women trained',250,180,'2026-09-30'),(10,4,'Micro-enterprises started',60,35,'2026-09-30'),
(11,5,'Families relieved',600,640,'2026-09-30'),(12,5,'Relief kits distributed',700,690,'2026-09-30'),
(13,6,'Borewells installed',15,13,'2026-09-30'),(14,6,'People with clean water access',5000,4200,'2026-09-30'),
(15,7,'Elders covered',150,150,'2025-03-31'),(16,7,'Health check-ups completed',300,285,'2025-03-31');
