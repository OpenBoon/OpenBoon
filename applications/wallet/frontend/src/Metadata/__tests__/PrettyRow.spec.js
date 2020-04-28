import TestRenderer from 'react-test-renderer'

import pdfAsset from '../../Asset/__mocks__/pdfAsset'
import bboxAsset, { boxImagesResponse } from '../../Asset/__mocks__/bboxAsset'

import MetadataPrettyRow from '../PrettyRow'

describe('<MetadataPrettyRow />', () => {
  it('should render documents', () => {
    require('swr').__setMockUseSWRResponse({ data: pdfAsset })
    const pdfContent = pdfAsset.metadata.media.content

    const component = TestRenderer.create(
      <MetadataPrettyRow name="content" value={pdfContent} path="media" />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render object detection', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...bboxAsset, ...boxImagesResponse },
    })
    const objectDetectionPredictions =
      bboxAsset.metadata.analysis['zvi-object-detection']

    const component = TestRenderer.create(
      <MetadataPrettyRow
        name="zvi-object-detection"
        value={objectDetectionPredictions}
        title="analysis"
        index={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render label detection', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...bboxAsset, ...boxImagesResponse },
    })
    const labelDetectionPredictions =
      bboxAsset.metadata.analysis['zvi-label-detection']

    const component = TestRenderer.create(
      <MetadataPrettyRow
        name="zvi-label-detection"
        value={labelDetectionPredictions}
        title="analysis"
        index={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render analysis with object value', () => {
    require('swr').__setMockUseSWRResponse({
      data: bboxAsset,
    })
    const textDetectionPredictions =
      bboxAsset.metadata.analysis['zvi-text-detection']

    const component = TestRenderer.create(
      <MetadataPrettyRow
        name="zvi-text-detection"
        value={textDetectionPredictions}
        title="analysis"
        index={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
