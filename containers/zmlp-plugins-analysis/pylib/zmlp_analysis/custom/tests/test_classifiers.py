import logging
import os
from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlpsdk.base import Frame
from zmlpsdk.storage import file_storage
from zmlp_analysis.custom.classifiers import NeuralNetClassifierProcessor
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()

assets = [
    TestAsset(
        "flowers/roses/99383371_37a5ac12a3_n.jpg",
        id="9f0f8a1d-4719-5cf8-b427-4612c5597811",
    )
]


class ClassifiersUnitTests(PluginUnitTestCase):
    ds_id = "ds-id-12345"
    model_id = "model-id-12345"
    base_dir = os.path.dirname(__file__)
    test_shash = (
        "0050FPPOGPPKPIPCAPPPDHHIGPCLJEPLPPPAPPGPDPFPPGPADCBFGIPGDDCIPIGPPDPABPOMPNPAPBPPDKP"
        "MPJMAPBPDPBOGLPDPPADOENPAAPBPPAPGAIGPDIPOGEPLCGAPPPPOAPBAJFMPPPDCPNKGAAMPLHIPEPDICO"
        "PLPIAEPACLJPCKMPGBPOLJAABLAPAPMMPOFPKPHEPLBPPMLPKFNPMKACECLPJPPABPPPAFPGIOEPBPAPPOH"
        "AFAHPPPJPPMDPAPPPELPAPCPPAKMLHJGPAGPMKPALPJPOMPPPPPJPPPAPPPLFINKPEBAAPADDPNMPLPPCBD"
        "PPOCCBCIPHDACPPCPHBDPBAPBPPALAPHPCFBCADAAPPHPHEPNHNCBOPDPPIPEABNHFPAPEPPDPPAAAPLMDP"
        "EAKPPPEGPAFPPBABDAOCKPPNPOPPGBGGPPKPMBPCDKCPBCDAPAPACAPPKHPGPPPPANPCEPCPJJGPPPEAPPA"
        "LPAIPEIICAGPPEPDDPNPFACMPGPBPDCFICPAPPPPCPCPNIGPBPNAGPPJEPEPPAHPAEAMKPBMLAPHPNPPDAK"
        "PGCPGPPPPPPABABPCPPANPPDEPHGCKFPAPPAPOEHNAAAKPDPNPOAGOPBPPPAPIPPIDDAPPPMAAPEMAIGAKG"
        "ABPPPGLPAPDOEHKIPPHPNPJCNPAGECLIIFAKPDIGDAPNPDPPIGACAOPACPBPBCAPPKPPIMPFKAALPAPGBAK"
        "AKPJPKDOAPPBPPPPPHIAPPBPPPPBFPPABACPGDJIPBPLIHPHPLPPIPLAPBCOGOJPGAPAGPDPMAMCNLGLHIL"
        "PMGDPAMAPJPHPPLPDFBDAKPLPPPKOPPGIFCPDJJPOPAPNAADAGPLGAEPMAFOPEPAHIDPDPBAJMEOGAPCGCA"
        "MPNCJFDPPGGDCMJPHMDPLGDAMIACPPPPPKPPIGPPPPOACPADPFPKBPNPCHPHBFAPONFKAEAPADPACAMLPPI"
        "PPDMPCPAPBCPCPJPJFFHPPCPGMPPFHPAMEAOIPPPDPPMAPJAHPPPPPCPAFPAAPEMAAMPPPBBAPDPLHDPPPB"
        "PPGHEEPDPDPPPEFGPBPBFEPLEPAAPEAPPEMAPPGJLGPPADNPPFMDPMFMPNPAOBMPAAICPGNPPJPPPDOPDBP"
        "ADAPHPPDAAAEHAPCPIAKLPGAHLPHDBABPHGPKBNFPPGPADPKEFPEEMPDJBIPAPBPCPBHPPCIODDAPLGPBDP"
        "APKBIPNBCIFHMDPAHPPPPCPPHABAAGPCPAACKFLJPPBAPPMPMOPAACJEPGPBEEHPPGPHPPIAPMPBEBPJBBF"
        "BPKPPHPPAPDECPPOBIPPIPBAAAPABGABPAPGGJJPAEEPBPABDEBPPODCPGDPKPPPAPPJDBKPOPABPPAIGGG"
        "PCBDPJBBPPAPPAPADEPPAPAPPFPPPPPPPCDPPLDIDBNPOECGPJPGLPPACPEPPPPPFFAPPBPCAMPKGPAGOPP"
        "PPGAAFPAPPLPEPHPKBAEJPPBDPPDPPIGOGPFPKBLMBPPAHPIJPLEAIHPPPBFBOEADBPFCDPPGAAPMKPNPCP"
        "MAPJPHEOPPIKEEOJPKEDJIEAIJPPPBNPEPPJJADFCKMCPADABEABPAPPAOBKDEIPAACPPDBEPPFPDCJPPCP"
        "CIABPAPHHPCLPMPNPPJAPNALPGKPOPAGPICGPGPAPPCPDPPGAFADLHIPPPOPPKPPAELAMLKACPIPPJBPBPA"
        "PCCAPHPPJPFPBKBPHGKDAPPPBAPHDAPMBPOABEPPPPHJNPAEPELCPPPPPPCCAJPBPNFGADHPPIKHGGBPAKF"
        "APODJCPPPPGCPPPPPPKAAAPKBNPPIHOPKPOHPPPAHJABEPKABGPGGDEAMPAAMFELPHILIPAPEHEPPPPPPLC"
        "APAPPPJPPIPBDGJPCBPKEGCHPJGJPPKPPPFBAPAFDHPFPEDHPLMPBJAPNPBAJBPAJEPJEAGPPFFPJFPLPIP"
        "JJEPPJPPEMGLKAAPPDCEPPLAGCEIEMAFPABPIPKPPPBAPKOPJAAKPABDFDCFB"
    )

    def prep_assets(self):
        for asset in assets:
            asset.set_attr(
                "files",
                [
                    {
                        "id": asset.id + ".jpg",
                        "mimetype": "image/jpeg",
                        "category": "proxy",
                        "attrs": {
                            "width": 100,
                            "height": 100,
                            "path": asset.get_attr("source.path"),
                        },
                    }
                ],
            )

            asset.set_attr("analysis.imageSimilarity.shash", self.test_shash)

        return assets

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("zmlp_analysis.custom.labels.get_proxy_level_path")
    def test_NeuralNetClassifier_defaults(
        self, proxy_patch, file_patch, model_patch
    ):
        assets = self.prep_assets()

        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        file_patch.return_value = "{}/{}.zip".format(self.base_dir, name)
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "dataSetId": self.ds_id,
                "type": "LABEL_DETECTION_MOBILENET2",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
            }
        )
        args = {
            "model_id": self.model_id,
        }

        for asset in assets:
            frame = Frame(asset)
            processor = self.init_processor(
                NeuralNetClassifierProcessor(), args
            )
            processor.process(frame)
            self.assertEquals(
                "rose",
                self.frame.asset.get_attr("analysis")["imageClassify"][
                    "pred0"
                ],
            )
