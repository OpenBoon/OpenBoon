import TestRenderer from 'react-test-renderer'

import pdfAsset from '../../Asset/__mocks__/pdfAsset'
import bboxAsset, { boxImagesResponse } from '../../Asset/__mocks__/bboxAsset'

import MetadataPrettyRow from '../PrettyRow'

describe('<MetadataPrettyRow />', () => {
  it('should render documents', () => {
    const pdfContent = pdfAsset.metadata.media.content

    const component = TestRenderer.create(
      <MetadataPrettyRow name="content" value={pdfContent} path="media" />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render object detection predictions', () => {
    require('swr').__setMockUseSWRResponse({
      data: boxImagesResponse,
    })
    const objectDetectionPredictions =
      bboxAsset.metadata.analysis['zvi-object-detection']

    const component = TestRenderer.create(
      <MetadataPrettyRow
        name="zvi-object-detection"
        value={objectDetectionPredictions}
        path="analysis"
        index={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render label detection predictions', () => {
    const labelDetectionPredictions =
      bboxAsset.metadata.analysis['zvi-label-detection']

    const component = TestRenderer.create(
      <MetadataPrettyRow
        name="zvi-label-detection"
        value={labelDetectionPredictions}
        path="analysis"
        index={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render image similarity', () => {
    const imageSimilarity = bboxAsset.metadata.analysis['zvi-image-similarity']

    const component = TestRenderer.create(
      <MetadataPrettyRow
        name="zvi-image-similarity"
        value={imageSimilarity}
        path="analysis"
        index={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render when value is of type object', () => {
    const textDetectionPredictions =
      bboxAsset.metadata.analysis['zvi-text-detection']

    const component = TestRenderer.create(
      <MetadataPrettyRow
        name="zvi-text-detection"
        value={textDetectionPredictions}
        path="analysis"
        index={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
