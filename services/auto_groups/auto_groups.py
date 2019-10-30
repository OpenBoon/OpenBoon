import archivist

import numpy as np
import os
import random
from sklearn.neighbors import KNeighborsClassifier

def rle_encode(data):
    encoding = ''
    prev_char = ''
    count = 1

    if not data: return ''

    for char in data:
        # If the prev and current characters
        # don't match...
        if char != prev_char:
            # ...then add the count and character
            # to our encoding
            if prev_char:
                encoding += str(count) + prev_char
            count = 1
            prev_char = char
        else:
            # Or increment our counter
            # if the characters do match
            count += 1
    else:
        # Finish off the encoding
        encoding += str(count) + prev_char
        return encoding



def read_hash_as_vectors(search=None,
                         attr='analysis.imageSimilarity.shash',
                         percentage=100,
                         colorAttr=None,
                         verbose=False):
    """This function reads a similarity hash from a Zorroa server into a numpy array.
    search: a search as returned bu archivist.AssetSearch(). If empty, an empty search will be used.
    attr: the attribute name of the hash to use.

    Returns a numpy array, a list of attribute IDs, a list of labels, and a list of legends
    """
    # If the provided search is None, create the empty search
    if search is None:
        search = archivist.AssetSearch()
    else:
        if type(search) != archivist.search.AssetSearch:
            print('ERROR: search must be of type archivist.search.AssetSearch.')
            return

    search = search.exists_filter(attr)

    if search.get_count() == 0:
        print('ERROR: no assets found with the given attributes. Nothing to do.')
        return

    if verbose:
        print("About to process %d assets" % int(search.get_count()*percentage/100.))

    random.seed(42)

    i = 0
    allData = []
    assets = []
    for a in search:
        if random.random()*100 < percentage:
            num_hash = []
            hash = a.get_attr(attr)
            for char in hash:
                num_hash.append(ord(char))
            allData.append(num_hash)
            assets.append(a)
            i += 1

    X = np.asarray((allData), dtype=np.float64)

    labels = []
    legend = []
    if colorAttr:
        dataTypes = []
        for a in assets:
            value = a.get_attr(colorAttr)
            if isinstance(value, (list,)):
                value = value[0]
            if value is None or value == '':
                value = "None"

            if value not in dataTypes:
                dataTypes.append(value)
            if value == "None":
                labels.append(-1)
            else:
                labels.append(dataTypes.index(value))
            legend.append(value)

    # labels and legend will be empty if no colorAttr was specified
    return X, assets


def getTrainingSet():
    trainData = []
    trainLabels = []
    trainChecksum = ''
    search = archivist.AssetSearch().exists_filter('analysis.autoGroups.training.keyword').exists_filter('analysis.imageSimilarity.shash')
    for a in search.scroll():
        keyword = a.get_attr('analysis.autoGroups.training.keyword')
        hash = a.get_attr('analysis.imageSimilarity.shash')
        trainChecksum += keyword
        num_hash = []
        for char in hash:
            num_hash.append(ord(char))
        trainData.append(num_hash)
        trainLabels.append(keyword)

    Xtrain = np.asarray((trainData), dtype=np.float64)
    Ytrain = np.asarray(trainLabels)
    trainChecksum = ''.join(sorted(trainChecksum))
    trainChecksum = rle_encode(trainChecksum)
    return trainChecksum, Xtrain, Ytrain



attr = 'analysis.imageSimilarity.shash'
folder = 'autoGroups'
nclusters = 50
label = None
value = None
archivist.client.connect('admin@cool', 'admin', 'http://localhost:8080')

P = 100
PERCENTAGE = P / 100.

print ("reading hashes")
inputSearch = archivist.AssetSearch().exists_filter(attr)
allData = []
allAssets = []
i = 0
for a in inputSearch:
    if random.random() < PERCENTAGE:
        allAssets.append(a)
        num_hash = []
        hash = a.get_attr(attr)
        for char in hash:
            num_hash.append(ord(char))
        allData.append(num_hash)
        i += 1
        if i % 1000 == 0:
            print (i)


oldTrainChecksum = None
oldPredictions = None
oldConf = None

confDict = {}
oldTrainChecksum = ''

while True:

    trainChecksum, Xtrain, Ytrain = getTrainingSet()

    if trainChecksum != oldTrainChecksum:
        oldTrainChecksum = trainChecksum
        print ('### Change detected -- Retraining')
    else:
        continue

    X = np.asarray((allData), dtype=np.float64)

    print ("classifying")
    classifier = KNeighborsClassifier(n_neighbors=1, p=1, weights='distance', metric='manhattan')
    classifier.fit(Xtrain, Ytrain)

    predictions = classifier.predict(X)

    dist, ind = classifier.kneighbors(X, n_neighbors=1, return_distance=True)
    conf = 1 - np.concatenate(dist) / np.max(np.concatenate(dist))

    # Set confidence predictions under the median for each category to "None".
    labels = list(set(predictions))
    for label in labels:
        threshold = np.median(conf[predictions==label]) * 0.9

        if label not in confDict.keys():
            confDict[label] = threshold
        else:
            if confDict[label] < threshold:
                threshold = confDict[label]
            else:
                confDict[label] = threshold

        #print (label, threshold)
        predictions[np.logical_and(predictions==label, conf < threshold)] = "None"

    if oldPredictions is None:
        oldPredictions = np.copy(predictions)
        oldPredictions[:] = 'xxx'
    if oldConf is None:
        oldConf = np.copy(conf)
        oldConf[:] = -1.0

    print ("writing results")
    i = 0
    myDict = {}
    for a in allAssets:

        if conf[i] != oldConf[i]:
            myDict[a] = {'analysis.autoGroups.predicted.keyword': predictions[i], 'analysis.autoGroups.predicted.conf': conf[i]}
            oldPredictions[i] = predictions[i]

        i += 1
        if i % 900 == 0:
            print (i)
            archivist.asset.set_attributes(myDict)
            myDict = {}
            trainChecksum, Xtrain, Ytrain = getTrainingSet()
            print (trainChecksum)
            if trainChecksum != oldTrainChecksum:
                print ('### Change detected -- Interrupting')
                break

    if myDict:
        print("writing...")
        archivist.asset.set_attributes(myDict)



    print ("Clean up...")
    i = 0
    myDict = {}
    for a in allAssets:
        myDict[a] = {'analysis.autoGroups.predicted.keyword': predictions[i], 'analysis.autoGroups.predicted.conf': conf[i]}

        i += 1
        if i % 900 == 0:
            print (i)
            archivist.asset.set_attributes(myDict)
            myDict = {}
            trainChecksum, Xtrain, Ytrain = getTrainingSet()
            if trainChecksum != oldTrainChecksum:
                print ('### Change detected -- Interrupting')
                break
    if myDict:
        print("writing...")
        archivist.asset.set_attributes(myDict)
