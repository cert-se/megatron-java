-- Retrieves number of rows and max id value for each table.  
-- Handy util script when creating a history database.   

select 'free_text', count(*), max(log_entry_id) from free_text;

select 'additional_item', count(*), max(log_entry_id) from additional_item;

select 'mail_job_log_entry_mapping', count(*), max(id), max(mail_job_id), max(log_entry_id) from mail_job_log_entry_mapping;

select 'mail_job', count(*), max(id), max(job_id) from mail_job;

select 'job', count(*), max(id) from job;

select 'log_entry', count(*), max(id), max(original_log_entry_id) from log_entry;

select 'original_log_entry', count(*), max(id) from original_log_entry;

select 'entry_type', count(*), max(id) from entry_type;

select 'job_type', count(*), max(id) from job_type;

select 'organization', count(*), max(id) from organization;

select 'prio', count(*), max(id) from prio;

select 'asn', count(*), max(id) from asn;

select 'domain_name', count(*), max(id) from domain_name;

select 'ip_range', count(*), max(id) from ip_range;

select 'asn_lookup', count(*) from asn_lookup;
