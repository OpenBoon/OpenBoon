from zorroa.zsdk import Frame, Generator, Asset, Document


class DocumentGenerator(Generator):
    def __init__(self, docs=None):
        super(DocumentGenerator, self).__init__()
        self.docs = docs or []

    def generate(self, consumer):
        for doc in self.docs:
            consumer.accept(Frame(Asset.from_document(Document(doc))))
