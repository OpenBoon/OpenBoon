from unittest import TestCase

from zmlp.elements import Element


class ElementTests(TestCase):
    stored_file = {
        'name': 'cat_object.jpg',
        'category': 'element',
        'attrs': {
            'width': 300,
            'height': 300
        }
    }

    def test_create_min_element(self):
        element = Element('object', 'cat')
        assert element.type == 'object'
        assert element.labels == ['cat']
        assert element.file is None
        assert element.rect is None
        assert element.score is None

    def test_create_rect_element_with_file(self):
        element = Element('object', 'cat', proxy_file=self.stored_file)

        assert element.type == 'object'
        assert element.labels == ['cat']
        assert element.file == 'element/cat_object.jpg'

    def test_create_rect_element_with_nw_region(self):
        element = Element('object', 'cat',
                          rect=[0, 0, 100, 100], proxy_file=self.stored_file)
        assert element.regions == ['NW']

    def test_create_rect_element_with_sw_region(self):
        element = Element('object', 'cat',
                          rect=[0, 175, 10, 200], proxy_file=self.stored_file)
        assert element.regions == ['SW']

    def test_create_rect_element_with_ne_region(self):
        element = Element('object', 'cat',
                          rect=[175, 0, 200, 10], proxy_file=self.stored_file)
        assert element.regions == ['NE']

    def test_create_rect_element_with_se_region(self):
        element = Element('object', 'cat',
                          rect=[200, 200, 300, 300], proxy_file=self.stored_file)
        assert element.regions == ['SE']

    def test_create_rect_element_with_center_region(self):
        element = Element('object', 'cat',
                          rect=[10, 10, 250, 250], proxy_file=self.stored_file)
        assert len(element.regions) == 5
        assert 'CENTER' in element.regions
