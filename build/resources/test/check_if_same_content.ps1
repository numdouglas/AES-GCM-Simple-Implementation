if((get-filehash my_doc.txt).hash -eq (get-filehash my_recovered_doc.txt).hash){write-host "Content Same"}
