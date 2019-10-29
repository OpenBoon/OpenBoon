#metadata plugin
This plugin contains the following processors:

###FilterKeywordsProcessor
Filter a list attribute using a dictionary.
Any keywords that are found in 'keywords' that also exist in 'dictionary' are copied
into 'filtered_keywords'.
    
###MapWordsProcessor
Read from an object's original_field.
Use map to apply changes.
Write to object's target_field.
'map' is a list of regular expression pairs
    
###ExtractWordsProcessor
Extract words longer than three characters and put them into an attribute.

###WordNetWordsProcessor
Extract all terms in an attribute that are words in Wordnet, put them in another attribute
Requires Wordnet data to be downloaded using nltk. This is necessary only once:


    python
    >> import nltk
    >> nltk.download('wordnet')
    
###ContentManagerProcessor
Processor that populates the content search for a Frame based on a list of it's
metadata fields.

###ExpandFrameCopyAttrsProcessor
Sets the metadata attributess an ExpandFrame should copy from it's parent Frame.

###MetadataRestRequestProcessor
(description needed)


###SplitStringProcessor
Splits a string or list of strings based on a provided delimiter.
