#include <jni.h>

#include "CaffeClassifier.h"

#define CPU_ONLY

#include <caffe/caffe.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <algorithm>
#include <iosfwd>
#include <memory>
#include <string>
#include <utility>
#include <vector>

using namespace caffe;  // NOLINT(build/namespaces)
using std::string;

/* Pair (label, confidence) representing a prediction. */
typedef std::pair<string, float> Prediction;

/* Info required to construct Java CaffeKeyword objects. */
typedef struct JNICaffeKeyword {
    jclass cls;
    jmethodID constructortorID;
    jfieldID keywordID;
    jfieldID confidenceID;
} JNICaffeKeyword;

static void InitJNIKeyword(JNIEnv *env, JNICaffeKeyword &sJNICaffeKeyword) {
    sJNICaffeKeyword.cls = env->FindClass("com/zorroa/ingestors/CaffeKeyword");
    if (sJNICaffeKeyword.cls == NULL) {
        fprintf(stderr, "Failed to find CaffeKeyword class in Java environment\n");
        return;
    }
    sJNICaffeKeyword.constructortorID = env->GetMethodID(sJNICaffeKeyword.cls, "<init>", "()V");
    if (sJNICaffeKeyword.constructortorID == NULL) {
        fprintf(stderr, "Failed to find the CaffeKeyword ctorID\n");
        return;
    }
    sJNICaffeKeyword.keywordID = env->GetFieldID(sJNICaffeKeyword.cls, "keyword", "Ljava/lang/String;");
    sJNICaffeKeyword.confidenceID = env->GetFieldID(sJNICaffeKeyword.cls, "confidence", "F");
    fprintf(stderr, "Successfully initialized the CaffeKeyword class for use in CaffeClassifier native code\n");
}

void SetJNIKeyword(JNIEnv *env, JNICaffeKeyword &sJNICaffeKeyword, jobject jKeyword, string keyword, float confidence) {
    env->SetObjectField(jKeyword, sJNICaffeKeyword.keywordID, env->NewStringUTF(keyword.c_str()));
    env->SetFloatField( jKeyword, sJNICaffeKeyword.confidenceID, (jfloat)confidence);
}

class Classifier {
 public:
  Classifier(const string &model_file, const string &trained_file, const string &mean_file, const string &label_file);

  std::vector<Prediction> Classify(const cv::Mat &img, int N = 5);

 private:
  void SetMean(const string &mean_file);

  std::vector<float> Predict(const cv::Mat &img);

  void WrapInputLayer(std::vector<cv::Mat>* input_channels);

  void Preprocess(const cv::Mat &img, std::vector<cv::Mat>* input_channels);

 private:
  shared_ptr<Net<float> > net_;
  cv::Size input_geometry_;
  int num_channels_;
  cv::Mat mean_;
  std::vector<string> labels_;
};

static string stringFromJString(JNIEnv *env, jstring jstr) {
    if (!jstr) {
        return string("");
    }

    const char *s = env->GetStringUTFChars(jstr, NULL);
    string str(s);
    env->ReleaseStringUTFChars(jstr, s);
    return str;
}

static std::vector<std::string> &split(const std::string &s, char delim, std::vector<std::string> &elems) {
    std::stringstream ss(s);
    std::string item;
    while (std::getline(ss, item, delim)) {
        elems.push_back(item);
    }
    return elems;
}

static std::vector<std::string> split(const std::string &s, char delim) {
    std::vector<std::string> elems;
    split(s, delim, elems);
    return elems;
}

static std::string trim(const std::string& str, const std::string& whitespace = " \t") {
    const size_t strBegin = str.find_first_not_of(whitespace);
    if (strBegin == std::string::npos)
        return ""; // no content
    const size_t strEnd = str.find_last_not_of(whitespace);
    const size_t strRange = strEnd - strBegin + 1;
    return str.substr(strBegin, strRange);
}

/*
 * Class:     com_zorroa_ingestors_CaffeClassifier
 * Method:    createCaffeClassifier
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_com_zorroa_ingestors_CaffeClassifier_createCaffeClassifier(JNIEnv *env, jclass nclass, jstring deployObj, jstring modelObj, jstring meanObj, jstring wordObj) {
    static bool initialized = false;
    if (!initialized) {
        initialized = true;
        google::InitGoogleLogging("ingestors");
    }
    Classifier *classifier = new Classifier(stringFromJString(env, deployObj), stringFromJString(env, modelObj), stringFromJString(env, meanObj), stringFromJString(env, wordObj));
    return (long)classifier;
}

/*
 * Class:     com_zorroa_ingestors_CaffeClassifier
 * Method:    classify
 * Signature: (JJ)[Lcom/zorroa/ingestors/CaffeKeyword;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_zorroa_ingestors_CaffeClassifier_classify(JNIEnv *env, jclass nclass, jlong jclassifier, jlong nativeMat) {
    Classifier *classifier = (Classifier *)jclassifier;
    cv::Mat *image = (cv::Mat *)nativeMat;
    CHECK(!image->empty()) << "Empty OpenCV matrix image";

    std::vector<Prediction> predictions = classifier->Classify(*image);
    std::vector<Prediction> keywords;

    // Sanitize the prediction keywords, removing the imagenet category and expanding synonyms
    for (size_t i = 0; i < predictions.size(); i++) {
        const Prediction &p = predictions[i];
        string multiwords = p.first;
        multiwords = multiwords.substr(multiwords.find_first_of(" \t") + 1);    // remove imagenet category eg. n182736
        std::vector<string> words = split(multiwords, ',');                     // split into separate keywords
        for (size_t j = 0; j < words.size(); ++j) {
            keywords.push_back(std::make_pair(trim(words[j]), p.second));
        }
    }

    // Initialize the return class on every call.
    // Overkill, but we definitely need to do it separately for each thread,
    // or we get crashes in NewObject or NewObjectArray.
    JNICaffeKeyword sJNICaffeKeyword;
    InitJNIKeyword(env, sJNICaffeKeyword);

    // Make sure we have enough local references to fill out our array.
    // We delete local refs after storing in the array, so this is probably overkill,
    // but, better safe than sorry!
    env->EnsureLocalCapacity(keywords.size() + 1 /*array*/);

    jobjectArray jKeywords = env->NewObjectArray(keywords.size(), sJNICaffeKeyword.cls, NULL);
    for (size_t i = 0; i < keywords.size(); i++) {
        const Prediction &p = keywords[i];
        jobject jKeyword = env->NewObject(sJNICaffeKeyword.cls, sJNICaffeKeyword.constructortorID);
        SetJNIKeyword(env, sJNICaffeKeyword, jKeyword, p.first, p.second);
        env->SetObjectArrayElement(jKeywords, i, jKeyword);

        // After storing the object in the array, we can release our local reference
        // which reduces the chance of hitting the limit of local references.
        env->DeleteLocalRef(jKeyword);
    }

    return jKeywords;
}

/*
 * Class:     com_zorroa_ingestors_CaffeClassifier
 * Method:    destroyCaffeClassifier
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_zorroa_ingestors_CaffeClassifier_destroyCaffeClassifier(JNIEnv *env, jclass nclass, jlong jclassifier) {
    Classifier *classifier = (Classifier *)jclassifier;
    delete classifier;
}

Classifier::Classifier(const string &model_file,
                       const string &trained_file,
                       const string &mean_file,
                       const string &label_file) {
#ifdef CPU_ONLY
  Caffe::set_mode(Caffe::CPU);
#else
  Caffe::set_mode(Caffe::GPU);
#endif

  /* Load the network. */
  net_.reset(new Net<float>(model_file, TEST));
  net_->CopyTrainedLayersFrom(trained_file);

  CHECK_EQ(net_->num_inputs(), 1) << "Network should have exactly one input.";
  CHECK_EQ(net_->num_outputs(), 1) << "Network should have exactly one output.";

  Blob<float>* input_layer = net_->input_blobs()[0];
  num_channels_ = input_layer->channels();
  CHECK(num_channels_ == 3 || num_channels_ == 1)
    << "Input layer should have 1 or 3 channels.";
  input_geometry_ = cv::Size(input_layer->width(), input_layer->height());

  /* Load the binaryproto mean file. */
  SetMean(mean_file);

  /* Load labels. */
  std::ifstream labels(label_file.c_str());
  CHECK(labels) << "Unable to open labels file " << label_file;
  string line;
  while (std::getline(labels, line))
    labels_.push_back(string(line));

  Blob<float>* output_layer = net_->output_blobs()[0];
  CHECK_EQ(labels_.size(), output_layer->channels())
    << "Number of labels is different from the output layer dimension.";
}

static bool PairCompare(const std::pair<float, int> &lhs,
                        const std::pair<float, int> &rhs) {
  return lhs.first > rhs.first;
}

/* Return the indices of the top N values of vector v. */
static std::vector<int> Argmax(const std::vector<float> &v, int N) {
  std::vector<std::pair<float, int> > pairs;
  for (size_t i = 0; i < v.size(); ++i)
    pairs.push_back(std::make_pair(v[i], i));
  std::partial_sort(pairs.begin(), pairs.begin() + N, pairs.end(), PairCompare);

  std::vector<int> result;
  for (int i = 0; i < N; ++i)
    result.push_back(pairs[i].second);
  return result;
}

/* Return the top N predictions. */
std::vector<Prediction> Classifier::Classify(const cv::Mat &img, int N) {
  std::vector<float> output = Predict(img);

  std::vector<int> maxN = Argmax(output, N);
  std::vector<Prediction> predictions;
  for (int i = 0; i < N; ++i) {
    int idx = maxN[i];
    predictions.push_back(std::make_pair(labels_[idx], output[idx]));
  }

  return predictions;
}

/* Load the mean file in binaryproto format. */
void Classifier::SetMean(const string &mean_file) {
  BlobProto blob_proto;
  ReadProtoFromBinaryFileOrDie(mean_file.c_str(), &blob_proto);

  /* Convert from BlobProto to Blob<float> */
  Blob<float> mean_blob;
  mean_blob.FromProto(blob_proto);
  CHECK_EQ(mean_blob.channels(), num_channels_)
    << "Number of channels of mean file doesn't match input layer.";

  /* The format of the mean file is planar 32-bit float BGR or grayscale. */
  std::vector<cv::Mat> channels;
  float* data = mean_blob.mutable_cpu_data();
  for (int i = 0; i < num_channels_; ++i) {
    /* Extract an individual channel. */
    cv::Mat channel(mean_blob.height(), mean_blob.width(), CV_32FC1, data);
    channels.push_back(channel);
    data += mean_blob.height() * mean_blob.width();
  }

  /* Merge the separate channels into a single image. */
  cv::Mat mean;
  cv::merge(channels, mean);

  /* Compute the global mean pixel value and create a mean image
   * filled with this value. */
  cv::Scalar channel_mean = cv::mean(mean);
  mean_ = cv::Mat(input_geometry_, mean.type(), channel_mean);
}

std::vector<float> Classifier::Predict(const cv::Mat &img) {
  Blob<float>* input_layer = net_->input_blobs()[0];
  input_layer->Reshape(1, num_channels_, input_geometry_.height, input_geometry_.width);
  /* Forward dimension change to all layers. */
  net_->Reshape();

  std::vector<cv::Mat> input_channels;
  WrapInputLayer(&input_channels);

  Preprocess(img, &input_channels);

  net_->ForwardPrefilled();

  /* Copy the output layer to a std::vector */
  Blob<float>* output_layer = net_->output_blobs()[0];
  const float* begin = output_layer->cpu_data();
  const float* end = begin + output_layer->channels();
  return std::vector<float>(begin, end);
}

/* Wrap the input layer of the network in separate cv::Mat objects
 * (one per channel). This way we save one memcpy operation and we
 * don't need to rely on cudaMemcpy2D. The last preprocessing
 * operation will write the separate channels directly to the input
 * layer. */
void Classifier::WrapInputLayer(std::vector<cv::Mat>* input_channels) {
  Blob<float>* input_layer = net_->input_blobs()[0];

  int width = input_layer->width();
  int height = input_layer->height();
  float* input_data = input_layer->mutable_cpu_data();
  for (int i = 0; i < input_layer->channels(); ++i) {
    cv::Mat channel(height, width, CV_32FC1, input_data);
    input_channels->push_back(channel);
    input_data += width * height;
  }
}

void Classifier::Preprocess(const cv::Mat &img,
                            std::vector<cv::Mat>* input_channels) {
  /* Convert the input image to the input image format of the network. */
  cv::Mat sample;
  if (img.channels() == 3 && num_channels_ == 1)
    cv::cvtColor(img, sample, CV_BGR2GRAY);
  else if (img.channels() == 4 && num_channels_ == 1)
    cv::cvtColor(img, sample, CV_BGRA2GRAY);
  else if (img.channels() == 4 && num_channels_ == 3)
    cv::cvtColor(img, sample, CV_BGRA2BGR);
  else if (img.channels() == 1 && num_channels_ == 3)
    cv::cvtColor(img, sample, CV_GRAY2BGR);
  else
    sample = img;

  cv::Mat sample_resized;
  if (sample.size() != input_geometry_)
    cv::resize(sample, sample_resized, input_geometry_);
  else
    sample_resized = sample;

  cv::Mat sample_float;
  if (num_channels_ == 3)
    sample_resized.convertTo(sample_float, CV_32FC3);
  else
    sample_resized.convertTo(sample_float, CV_32FC1);

  cv::Mat sample_normalized;
  cv::subtract(sample_float, mean_, sample_normalized);

  /* This operation will write the separate BGR planes directly to the
   * input layer of the network because it is wrapped by the cv::Mat
   * objects in input_channels. */
  cv::split(sample_normalized, *input_channels);

  CHECK(reinterpret_cast<float*>(input_channels->at(0).data)
        == net_->input_blobs()[0]->cpu_data())
    << "Input channels are not wrapping the input layer of the network.";
}
