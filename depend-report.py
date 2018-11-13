#!/usr/bin/env python
import subprocess
import os

from BeautifulSoup import BeautifulSoup as bs

projects = [
    "ldap",
    "saml",
    "archivist",
    "lib-security",
    "es-similarity"
]


def main():

    dev_null = open(os.devnull, "wb")
    subprocess.check_call(["mvn", "project-info-reports:dependencies"], shell=False, stdout=dev_null, stderr=subprocess.STDOUT)

    result = {}

    for project in projects:
        doc = bs(open("%s/target/site/dependencies.html" % project))
        tables = doc.body.findAll("table", attrs={"class": ["bodyTable"]})

        for table in tables:
            rows = table.findAll("tr")
            for row in rows:
                cols = row.findAll("td")
                cols = [e.text.strip() for e in cols]
                if len(cols) != 5:
                    continue
            
                result["%s.%s" % (cols[0], cols[1])] = cols[-1]

    keys = result.keys()
    keys.sort()
    for k in keys:
       print "%s: %s" % (k, result[k])


if __name__ == "__main__":
    main()

