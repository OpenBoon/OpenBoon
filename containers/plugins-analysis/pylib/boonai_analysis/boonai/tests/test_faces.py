from unittest.mock import patch

from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, \
    get_prediction_labels
from boonai_analysis.zvi.faces import ZviFaceDetectionProcessor


class ZviFaceDetectionProcessorTests(PluginUnitTestCase):

    @patch('boonai_analysis.zvi.faces.get_proxy_level_path')
    def test_process_detection(self, proxy_patch):
        image_path = test_path('images/face-recognition/face1.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        processor = self.init_processor(ZviFaceDetectionProcessor(), {})
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.boonai-face-detection')
        grouped = get_prediction_labels(analysis)
        assert 'face0' in grouped
        assert 'labels' == analysis['type']
        assert analysis['predictions'][0]['simhash'] == 'JJJLMIOLPMONNKKOLLNOLMNLMLKGIP' \
                                                        'KLINOPNKKSLMKNJNNQNOPNJPNLMLIL' \
                                                        'OOOKPMMJOLNNLILNPJOOJLNKLNOLPM' \
                                                        'PNLLKMOLKPMHOMJOOPNNMJPOKJLOKO' \
                                                        'LIOPNLFOJNMHMKLPKOOONNMPNOOPLP' \
                                                        'LLMOJJTPLONKHMPNMLNIOLOPMLMKJN' \
                                                        'JNNNJPOLIKKJNNJKQPMOKLJMOKQLLN' \
                                                        'LKMJJMMNQNMIQKPOJIMMPQLOPOJLIM' \
                                                        'NJMNOPLPRJNNLMMKLILMNMMLKQKLHM' \
                                                        'KLHLLNNJKMPKNOJIJMKQMLNKLNJLLO' \
                                                        'LPNOMKMPKMNMNHNOMLLKNPPNLPONNR' \
                                                        'POMPOMMQONOMKLNNLMLHJMOJLMOJNM' \
                                                        'NQKMNLOMKMJONNHLLMMLPONKNOKLJQ' \
                                                        'OKJLMJLJMLOLKOLMNKKKIKNLKLNMMO' \
                                                        'MMMORONPNMNKPMJJPNGQJJNTOLMJLM' \
                                                        'NPLMLJLLLNNLLNJMPOMINRNOLNHKMK' \
                                                        'INSONRLMILOOMOOQNOOLNMMNQNKNKM' \
                                                        'ML'
