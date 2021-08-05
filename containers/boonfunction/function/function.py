# flake8: noqa

from boonsdk.func import FunctionResponse, LabelDetectionAnalysis

from . import support


def process(asset):
    rsp = FunctionResponse()

    labels1 = LabelDetectionAnalysis()
    labels1.add_label_and_score("cat", 0.1)
    labels1.add_label_and_score("dog", 0.2)
    labels1.add_label_and_score("hippo", 0.9)
    rsp.set_analysis(labels1)

    labels2 = LabelDetectionAnalysis()
    labels2.add_label_and_score("forest", 0.1)
    labels2.add_label_and_score("desert", 0.2)
    labels2.add_label_and_score("river", 0.9)
    rsp.add_more_analysis("setting", labels2)
    rsp.set_custom_field("movie-name", "Hippo Man")

    return rsp
