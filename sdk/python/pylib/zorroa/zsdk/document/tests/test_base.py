#!/usr/bin/env python

import unittest

from zorroa.zsdk.document.base import generate_asset_id


class BaseUnitTests(unittest.TestCase):

    def test_generate_asset_id(self):
        id1 = generate_asset_id("/foo/bar")
        id2 = generate_asset_id("/foo/bar")
        id3 = generate_asset_id("/foo/bar", "clip=123")

        assert id1 == id2
        assert id2 != id3

        # If these break that would be very bad, cause it means we'll
        # likely be generating duplicate assets.
        assert id1 == "db1e0b4b-ea8e-5507-8720-461ec30392ee"
        assert id3 == "289a5a8d-d93e-5e6a-959e-d2809e463a40"
