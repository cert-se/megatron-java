# Collection of handy SQL queries

-- List log entries for specified IP.
select *, FROM_UNIXTIME(log_entry.created), FROM_UNIXTIME(log_timestamp), INET_NTOA(ip_address), INET_NTOA(ip_address2), INET_NTOA(ip_range_start), INET_NTOA(ip_range_end)
from log_entry, original_log_entry
where 
  original_log_entry.id = log_entry.original_log_entry_id and
  (INET_ATON('192.121.218.90') between log_entry.ip_range_start and log_entry.ip_range_end or 
  log_entry.ip_address = INET_ATON('192.121.218.90'));


-- List log entries for specified IP (including additional items).
select *, FROM_UNIXTIME(log_entry.created), FROM_UNIXTIME(log_timestamp), INET_NTOA(ip_address), INET_NTOA(ip_address2), INET_NTOA(ip_range_start), INET_NTOA(ip_range_end)
from (log_entry, original_log_entry)
left join additional_item
on log_entry.id = additional_item.log_entry_id
where
  original_log_entry.id = log_entry.original_log_entry_id and
  (INET_ATON('192.121.218.90') between log_entry.ip_range_start and log_entry.ip_range_end or 
  log_entry.ip_address = INET_ATON('192.121.218.90'));


-- List log entries for specified job.
select *, FROM_UNIXTIME(log_entry.created), FROM_UNIXTIME(log_timestamp), INET_NTOA(ip_address), INET_NTOA(ip_address2)
from job, log_entry, original_log_entry
where 
  original_log_entry.id = log_entry.original_log_entry_id and
  log_entry.job_id = job.id and
  job.name = 'shadowserver-drone_2012-08-21_085407';


-- List log entries for specified organization.
select *, FROM_UNIXTIME(log_entry.created), FROM_UNIXTIME(log_timestamp), INET_NTOA(ip_address), INET_NTOA(ip_address2)
from organization, log_entry, original_log_entry
where 
  original_log_entry.id = log_entry.original_log_entry_id and
  log_entry.org_id = organization.id and
  organization.name like '%CERT%';


-- As above (list log entries for specified organization), but formatted for export.  
select FROM_UNIXTIME(log_entry.created) as created, FROM_UNIXTIME(log_timestamp) as log_timestamp, INET_NTOA(ip_address) as ip_address, 
  hostname, port, asn, log_entry.country_code, INET_NTOA(ip_address2) as ip_address2, hostname2, port2, asn2, log_entry.country_code2, 
  INET_NTOA(ip_range_start) as ip_range_start, INET_NTOA(ip_range_end) as ip_range_end, url, original_log_entry.entry
from log_entry, original_log_entry, organization
where 
  original_log_entry.id = log_entry.original_log_entry_id and
  log_entry.org_id = organization.id and
  organization.name like '%CERT%'
order by created;  


-- List organization and log entries in priority order for specified job and prio
select *, FROM_UNIXTIME(log_entry.created), FROM_UNIXTIME(log_timestamp), INET_NTOA(ip_address), INET_NTOA(ip_address2)
from organization, prio, log_entry, original_log_entry, job
where 
  original_log_entry.id = log_entry.original_log_entry_id and
  prio.prio > 40 and
  prio.id = organization.prio_id and
  organization.id = log_entry.org_id and
  log_entry.job_id = job.id and
  job.name = 'web-apache_20012-08-21_103556'
order by prio.prio desc, organization.name asc;


-- List log entries for specified job (including additional items).
select *, FROM_UNIXTIME(log_entry.created), FROM_UNIXTIME(log_timestamp), INET_NTOA(ip_address), INET_NTOA(ip_address2)
from (job, log_entry, original_log_entry)
left join additional_item
on log_entry.id = additional_item.log_entry_id
where 
  original_log_entry.id = log_entry.original_log_entry_id and
  log_entry.job_id = job.id and
  job.name = 'shadowserver-drone_2012-08-21_085407';

  
-- List log jobs processed the last 24 hours.
select *, FROM_UNIXTIME(started), FROM_UNIXTIME(finished) from job where started >= (unix_timestamp() - 24*60*60);


-- List log jobs from a specified date.
select *, FROM_UNIXTIME(started), FROM_UNIXTIME(finished) from job where started > UNIX_TIMESTAMP('2013-06-01 00:00:00') order by started;


-- List number of log entries per organization for a specified job 
select organization.name, prio, count(*) as "No. of Log Entries" 
from organization, prio, log_entry, job
where 
  organization.prio_id = prio.id and log_entry.org_id = organization.id and
  log_entry.job_id = job.id and job.name = 'shadowserver-sinkhole-http-drone_2012-12-10_120029'
group by name
order by prio desc;


-- List IP-ranges, hostnames, and ASNs for specified organization name
select *, INET_NTOA(start_address), INET_NTOA(end_address) 
from (organization left join domain_name on domain_name.org_id = organization.id)
left join asn on asn.org_id = organization.id
left join ip_range on ip_range.org_id = organization.id
where 
  name = 'CERT-SE';


-- List DNSChanger log entries for ISPs (13=ISP)
//select *, FROM_UNIXTIME(log_entry.created), FROM_UNIXTIME(log_timestamp), INET_NTOA(ip_address), INET_NTOA(ip_address2)
//select distinct INET_NTOA(ip_address), organization.name
select FROM_UNIXTIME(log_entry.log_timestamp), INET_NTOA(ip_address), hostname, port, INET_NTOA(ip_address2), organization.name
from (log_entry, additional_item, original_log_entry, organization)
where
  organization.id = log_entry.org_id and
  organization.prio_id = 13 and
  original_log_entry.id = log_entry.original_log_entry_id and
  additional_item.value like 'dns%' and
  additional_item.log_entry_id = log_entry.id and
  log_entry.log_timestamp > UNIX_TIMESTAMP('2012-03-26 00:00:00')
order by log_entry.ip_address, log_entry.log_timestamp desc;


-- List number of log entries per organization last 2 weeks for ISP + web hotels 
select organization.name, prio, count(*) as "No. of Log Entries"
from organization, prio, log_entry, job
where
  prio.name in ('ISP', 'Webbhotell')  and
  organization.prio_id = prio.id and
  log_entry.org_id = organization.id and
  log_entry.job_id = job.id and
  job.started >= (unix_timestamp() - 24*14*60*60)
group by name
order by prio desc;
