gazetteer-postproc:

= JAPE:removeLookups
  - removes Lookups which are fully contained in another lookup
    This will remove candidate lists for positions in the text 
    where a shorter and a longer label match. 
  - removes Lookups, Locations, Organizations if they overlap in any 
    way with an ANNIE "Date" or ANNIE "Address.kind=url"
