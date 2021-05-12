import TestRenderer, { act } from 'react-test-renderer'

import bboxAsset, { boxImagesResponse } from '../../Asset/__mocks__/bboxAsset'
import assets from '../../Assets/__mocks__/assets'

import MetadataPrettySwitch from '../Switch'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = assets.results[0].id

describe('<MetadataPrettySwitch />', () => {
  it('should render an empty row properly', () => {
    const component = TestRenderer.create(
      <MetadataPrettySwitch name="" value="" path="files.attrs" />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

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
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        assetId: ASSET_ID,
        projectId: PROJECT_ID,
        query: btoa(
          JSON.stringify([
            {
              type: 'labelConfidence',
              attribute: 'analysis.boonai-label-detection',
              values: {
                labels: ['banana'],
                min: 0.25,
                max: 0.75,
              },
            },
          ]),
        ),
      },
    })

    const value = bboxAsset.metadata.analysis['boonai-label-detection']

    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-label-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ children: 'cherry' }).props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelConfidence',
          attribute: 'analysis.boonai-label-detection',
          values: {
            labels: ['cherry'],
            min: 0.25,
            max: 0.75,
          },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })

  it('should render label with no predictions properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { assetId: ASSET_ID, projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-label-detection"
        value={{ type: 'labels', predictions: [] }}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'boonai-label-detection' })
        .props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelConfidence',
          attribute: 'analysis.boonai-label-detection',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })

  it('should render label detection with box images properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { assetId: ASSET_ID, projectId: PROJECT_ID },
    })

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

    act(() => {
      component.root
        .findByProps({ children: 'boonai-object-detection' })
        .props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelConfidence',
          attribute: 'analysis.boonai-object-detection',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })

  it('should render content detection properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

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

    act(() => {
      component.root
        .findByProps({ children: 'boonai-text-detection' })
        .props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: 'analysis.boonai-text-detection',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })

  it('should render content detection with no results properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const value = bboxAsset.metadata.analysis['boonai-text-detection']

    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-text-detection"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'boonai-text-detection' })
        .props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: 'analysis.boonai-text-detection',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })

  it('should render similarity detection properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { assetId: ASSET_ID, projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: assets })

    const value = bboxAsset.metadata.analysis['boonai-image-similarity']

    const component = TestRenderer.create(
      <MetadataPrettySwitch
        name="boonai-image-similarity"
        value={value}
        path="analysis"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'boonai-image-similarity' })
        .props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'similarity',
          attribute: 'analysis.boonai-image-similarity',
          values: {
            ids: [ASSET_ID],
            minScore: 0.75,
          },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
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
