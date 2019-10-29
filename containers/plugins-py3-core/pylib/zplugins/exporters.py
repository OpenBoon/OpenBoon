import json

from zsdk import AbstractExporter, Argument


class JsonExporter(AbstractExporter):
    """An Exporter for writing a JSON file of metadata."""
    toolTips = {
        'file_name': 'If left unset, the filename defaults to assets.json',
        'fields': 'Fields to export'
    }

    def __init__(self):
        super(JsonExporter, self).__init__()
        self.add_arg(Argument('file_name', 'str', default='assets.json',
                              toolTip=self.toolTips['file_name']))
        self.add_arg(Argument('fields', 'list', required=True, toolTip=self.toolTips['fields']))
        self.json_fp = None

        # This flag is used to check whether we are writing entries after the first one
        self.multiple = False

    def init(self):
        json_path = self.export_root_dir.joinpath(self.arg_value("file_name"))
        self.json_fp = open(str(json_path), "a")
        self.json_fp.write("[\n")

    def export(self, frame):
        dictionary = {}
        for field in self.arg_value("fields"):
            dictionary = self.set_keys(frame, dictionary, field, field)

        if self.multiple:
            self.json_fp.write(",")
        self.multiple = True

        self.json_fp.write(json.dumps(dictionary, indent=4))

    def teardown(self):
        if self.json_fp:
            self.json_fp.write(" ] ")
            self.json_fp.close()

    def set_keys(self, frame, dictionary, field, full_field):
        """We do this recursively in order to create a dict with the right structure"""
        path = field.split('.')
        if len(path) > 1:
            if path[0] not in dictionary:
                dictionary[path[0]] = {}
            dictionary[path[0]] = self.set_keys(frame, dictionary[path[0]], '.'.join(path[1:]),
                                                full_field)
        else:
            if isinstance(dictionary, dict):
                dictionary[field] = frame.asset.get_attr(full_field)
        return dictionary


class CsvExporter(AbstractExporter):
    """An Exporter for writing a CSV file of metadata."""
    toolTips = {
        'file_name': 'If left unset, the filename defaults to assets.csv',
        'fields': 'Fields to export'
    }

    def __init__(self):
        super(CsvExporter, self).__init__()
        self.add_arg(Argument('file_name', 'str', default='assets.csv',
                              toolTip=self.toolTips['file_name']))
        self.add_arg(Argument('fields', 'list', required=True, toolTip=self.toolTips['fields']))
        self.csv_fp = None

    def init(self):
        csv_path = self.export_root_dir.joinpath(self.arg_value("file_name"))
        self.csv_fp = open(str(csv_path), "a")
        self.write_csv_header()

    def export(self, frame):
        line = []
        for field in self.arg_value("fields"):
            value = frame.asset.get_attr(field)
            if value:
                if type(value) == unicode:
                    value = value.encode('utf-8')
                elif isinstance(value, (list, tuple, set)):
                    value = ' '.join([str(i) for i in value])
                else:
                    value = str(value)
                if "," in value:
                    value = '"%s"' % value
            else:
                value = ""
            line.append(value)
        self.csv_fp.write(",".join(line) + "\n")

    def teardown(self):
        if self.csv_fp:
            self.csv_fp.close()

    def write_csv_header(self):
        header = []
        for field in self.arg_value("fields"):
            header.append(field)
        self.csv_fp.write(",".join(header).encode('utf-8') + "\n")
