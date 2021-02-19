import TestRenderer from 'react-test-renderer'

import bboxAsset, { boxImagesResponse } from '../../Asset/__mocks__/bboxAsset'
import assets from '../../Assets/__mocks__/assets'

import MetadataPrettySwitch from '../Switch'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = assets.results[0].id

describe('<MetadataPrettySwitch />', () => {
  it('should render regular text values', () => {
    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="not-content"
        value="Lorem Ipsum Cupcake Sugar Plum"
        path="media"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render long content text', () => {
    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="content"
        value={'Lorem Ipsum Cupcake Sugar Plum'.repeat(12)}
        path="media"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render label with no box images detection properly', () => {
    const value = bboxAsset.metadata.analysis['boonai-label-detection']
    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-label-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render single-label predictions properly', () => {
    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="animal"
        value={{ label: 'Horse', score: 0.998, type: 'single-label' }}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render label with no predictions properly', () => {
    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-label-detection"
        value={{ type: 'labels', predictions: [] }}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render label detection with box images properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: boxImagesResponse,
    })

    const value = bboxAsset.metadata.analysis['boonai-object-detection']
    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-object-detection"
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
      <MetadataPrettySwitch
        name="boonai-text-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render content detection with no results properly', () => {
    const value = bboxAsset.metadata.analysis['boonai-text-detection']
    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-text-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render similarity detection properly', () => {
    require('swr').__setMockUseSWRResponse({ data: assets })

    require('next/router').__setUseRouter({
      query: { assetId: ASSET_ID, projectId: PROJECT_ID },
    })

    const value = bboxAsset.metadata.analysis['boonai-image-similarity']

    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-image-similarity"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render similarity detection with no data properly', () => {
    require('swr').__setMockUseSWRResponse({ data: null })

    require('next/router').__setUseRouter({
      query: { assetId: ASSET_ID, projectId: PROJECT_ID },
    })

    const value = bboxAsset.metadata.analysis['boonai-image-similarity']

    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-image-similarity"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
