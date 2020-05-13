import TestRenderer from 'react-test-renderer'

import bboxAsset, { boxImagesResponse } from '../../Asset/__mocks__/bboxAsset'

import MetadataPrettyObject from '../Object'

describe('<MetadataPrettyObject />', () => {
  it('should render label with no box images detection properly', () => {
    const value = bboxAsset.metadata.analysis['zvi-label-detection']
    const component = TestRenderer.create(
      <MetadataPrettyObject
        name="zvi-label-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render label detection with box images properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: boxImagesResponse,
    })

    const value = bboxAsset.metadata.analysis['zvi-object-detection']
    const component = TestRenderer.create(
      <MetadataPrettyObject
        name="zvi-object-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render content detection properly', () => {
    const value = {
      count: 2,
      type: 'content',
      content: 'some result',
    }
    const component = TestRenderer.create(
      <MetadataPrettyObject
        name="zvi-text-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render content detection with no results properly', () => {
    const value = bboxAsset.metadata.analysis['zvi-text-detection']
    const component = TestRenderer.create(
      <MetadataPrettyObject
        name="zvi-text-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render similarity detection properly', () => {
    const value = bboxAsset.metadata.analysis['zvi-image-similarity']
    const component = TestRenderer.create(
      <MetadataPrettyObject
        name="zvi-image-similarity"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
