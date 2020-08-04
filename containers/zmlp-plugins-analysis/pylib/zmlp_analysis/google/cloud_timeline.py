import zmlpsdk.timeline as ztl


def build_text_detection_timeline(annotations):
    """
    Build a timeline for video text detection.

    Args:
        annotations (AnnotateVideoResponse):

    Returns:
        Timeline: The populated Timeline.

    """
    timeline = ztl.Timeline("gcp-video-text-detection")
    track = timeline.add_track("Detected Text")

    for annotation in annotations.text_annotations:
        for segment in annotation.segments:
            start_time = convert_offset(segment.segment.start_time_offset)
            end_time = convert_offset(segment.segment.end_time_offset)
            track.add_clip(start_time, end_time, {"content": annotation.text})

    return timeline


def build_object_detection_timeline(annotations):
    """
    Build a timeline for video object detection.

    Args:
        annotations (AnnotateVideoResponse):

    Returns:
        Timeline: The populated Timeline.

    """
    timeline = ztl.Timeline("gcp-video-object-detection")
    track = timeline.add_track("Detected Object")

    for annotation in annotations.object_annotations:
        start_time = convert_offset(annotation.segment.start_time_offset)
        end_time = convert_offset(annotation.segment.end_time_offset)
        track.add_clip(start_time, end_time, {"content": annotation.entity.description})

    return timeline


def build_content_moderation_timeline(annotations):
    """
    Use the explicit annotation to build a timeline.

    Args:
        annotations (AnnotateVideoResponse):

    Returns:

    """
    timeline = ztl.Timeline("gcp-video-explicit-detection")
    tracks = [
        None,
        timeline.add_track("Very Unlikely", sort=5),
        timeline.add_track("Unlikely", sort=3),
        timeline.add_track("Possible", sort=3),
        timeline.add_track("Likely", sort=2),
        timeline.add_track("Very Likely", sort=1)
    ]

    current_clip = None
    previous_frame = None

    for frame in annotations.explicit_annotation.frames:
        scrubber = convert_offset(frame.time_offset)

        # Close the current clip of this happens.
        if previous_frame and previous_frame.pornography_likelihood != frame.pornography_likelihood:
            if current_clip:
                current_clip.extend_to(scrubber)
                current_clip = None

        if frame.pornography_likelihood > 0:
            if not current_clip:
                current_clip = tracks[frame.pornography_likelihood].add_clip(scrubber, scrubber)
            else:
                current_clip.extend_to(scrubber)

        previous_frame = frame

    return timeline


def build_label_detection_timeline(annotations):
    """
    Use label annotations to build a label detection timeline.

    Args:
        annotations (AnnotateVideoResponse):

    Returns:
        Timeline: The Label Detection timeline.
    """
    timeline = ztl.Timeline("gcp-video-label-detection")

    for annotation in annotations.shot_presence_label_annotations:
        labels = set([annotation.entity.description])

        for category in annotation.category_entities:
            labels.add(category.description)

        for seg in annotation.segments:
            if not seg.segment.start_time_offset:
                clip_start = 0
            else:
                clip_start = convert_offset(seg.segment.start_time_offset)

            clip_stop = convert_offset(seg.segment.end_time_offset)

        for label in labels:
            track = timeline.add_track(label)
            track.add_clip(clip_start, clip_stop)

    return timeline


def convert_offset(offset):
    """
    Convert the GCP seconds/nanos to a float.

    Args:
        offset (object): A GCP time object.

    Returns:
        float: A float representing seconds and nanos.

    """
    return offset.seconds + (offset.nanos / 1000000000.0)
