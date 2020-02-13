#!/usr/bin/env python3

from zmlp import app_from_env
from zmlp.search import SimilarityQuery

h = "PBPDIPPKAPPPGPIHPLPLLMPHAPCPEPPBAPJBMLMBPPPMMPNABNGPMEPPIABGGPNPEBCHPPODAPPCJPHLPPCELPJPPMAF" \
    "HFPFBCGFBFPHJPDDAEPOPPDJKEHPHPPECPDCJHPPBPGPJCENLGOHPAMAPDAPPFBPAJPALHOPCGPPIKPEOGILPLPEOLMD" \
    "PGPPNIHPKCIAABPDAGKGJPPPGGCALPKOLPFHPFBPAPPJBBPIPNPFMEPAPCIPPPAKPEGGPEDPMCPAPMPCLNJPPCPPGJCI" \
    "IPKIPLPPOPEMPPPGPPJCJOPPPPPHPPPEPPIPPPKPMPNLPEGPMFPPGPFNPPEBFCPHPLPBBPJPMEPKPDMKALPOEPLGPHNP" \
    "PHDJEPOPPPPPCEPPJEEGPCPPDPBBLELGPBPPHPPPPCPPJEPPDPBPDPJPAFCFJPAPEBCPJGPPPPPPMMKFPJPPDBEJPFPM" \
    "HPOJDAPCLPJPPPPJDBKMGGBPPPBGJEEMPDOOPBPECPJLHPIIPPKDGPPFPAJCLDPFPPPLPDNPPBPPPBNDFPCJPFPIPDDP" \
    "PPCPIOPPPPAPPEGPNACPPPPLPEPEGIPIAPPIKOLIEBPLCPCHAPBPPIBPAAPPPAPMMPPFPOPKIPIBAPPLDBDKPCLNPNOP" \
    "PIODGOOPPGFFACPHADPCPLINPGBPCPDMPFDCKIKDBAEPHHECPPHOPFFFMMPAPNEFLDPPPBKPKGAPKGDPKFCPPEIPCPNO" \
    "CPAPFIPOPMDFJPPPPKALPPJPBPAAIPNPFPCDOBPPPBHICPAPPDIPPPPJPPHGBPPPIJPEGPNPPIEAEPMPFBPIPDPPPILP" \
    "GPPMPJFFPADPGPDGPGGEBCMPIHPPPDPPPAPPALOPPDFNPPPJFPPPAGPPPEBIFPGDPNPPDDJPPHEPFPDIDGIOPPLPBEPP" \
    "APKPPKPPIIIKGOPPAPPLPKCPKMPEGOLAAPIPPEFGIPFPPJKPAPPLPHNCHJJNDCPBPPOPBNAPMPNPEPGGCPPPPJEPPPFH" \
    "PCDAPDMPGJPPNJKPPPCPMAPPGGGPIDGPICPNPLMKOPPNOPBDAHEIKPPPPPPDFAGCDPPPBDAPEHPAIBPCPPPHEPPODPPN" \
    "OPFMFPDOPPPAPIIPPPPPPPIPIPCOPCGNGPANCPKEPJPPFEAIJGDDMPAPBPNFAFIPOPNBPBMPMPPPPFKBLAPPDMECPIHB" \
    "HIBPJPAPPPPPEEAMCNPPPICJJMPKHPPIDBCPIPPAJOFPIPAGFBPFKPPPFPPAPPAIPPOALABHDLPMCMEBPBPPCGKPPPPP" \
    "PFFHCMPDLEPPFPKPPPPEFPEPPAPPLBMCNPAPPPKPPDFPPPECPPLFMPAHHMFPECPKOPDFIPEPPJGPPIEODBPPBPFPLPPP" \
    "PFFDBEPDPBMLJECLPDJPMPFPKLJMAPPPIFPPHFBHPLNDIPKAAPDIPFPNPIPPJCPPPDPCAEJPPPOPIPAIPPMGKPPFJGPH" \
    "PFLPKKPPPNPPPAEAPPPJNCPOJDPLGPCEHPOPPFDNAPPPPPBPODPCLPDPDCMLPKPPFPCPPEPIEIFDHFHPPHOJDGPHPAJP" \
    "PPPPAGPPOPPPBAMPPPKJPPPEAPEBPELHHBIPMAPPKPGKALPGEFPPDPCAAPPPPMFKLFGDJIIDPPDHIPHPPDNEPPBCPFPP" \
    "BIMPBPPLJAHBPJPPHJPOPPPPPPOCBPPPKPHHPCPOAOEPBPFFPPHNPPPFPJCPOPPBPPOPOFAPPIHLEJPNLHPPPCPPPBNJ" \
    "APPPPPLPIPAEPHDPLPPJGGPPAPEEJFFPGPPPLPPBFPPBCEKPPPLGLABDPNJMKPHAPPEPECPGPOIJPDPMPKPCLDPPPPLN" \
    "DDPBADPPPJECGAKGPPMPPCIEPPCLPAFPPPINMAFPBPIMJFDPJNPFCPKPHBABHAPFCFFPDAACAPFPPDPIPPAACPPPOEPC" \
    "PLBGAPPFPPPPEFPAOAFPPPKPDDPPEHPKPPDECPEFPIPPGOPPPGPPPPJAPPPPAPFPDPLPPPAPPPLPOPHIBJPPPJOLPPAP" \
    "PPPPPPBAJABEPNOPPFPBPJFP"

q = {
    "query": {
        "bool": {
            "must": [
                SimilarityQuery("analysis.zmlp.similarity.vector", 0.60, h)
            ]
        }
    }
}

app = app_from_env()
search = app.assets.search(q)
for a in search:
    print(a)
