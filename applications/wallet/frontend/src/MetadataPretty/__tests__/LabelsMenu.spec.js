import TestRenderer, { act } from 'react-test-renderer'

import MetadataPrettyLabelsMenu from '../LabelsMenu'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
const MODEL_ID = '621bf774-89d9-1244-9596-d6df43f1ede5'

describe('<MetadataPrettyLabelsMenu />', () => {
  it('should add a model/label filter in the grid', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsMenu
        label={{
          modelId: MODEL_ID,
          scope: 'TRAIN',
          bbox: [0.226, 0.327, 0.697, 0.813],
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMKKKM...',
          b64_image: 'data:image/png;base64, iVBORw0KGgoAAAANSUhEU...',
        }}
        moduleName="zvi-face-recognition"
      />,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Select Add Filter
    act(() => {
      component.root
        .findByProps({ children: 'Add Model/Label Filter' })
        .props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: PROJECT_ID,
          id: ASSET_ID,
          query: btoa(
            JSON.stringify([
              {
                type: 'label',
                attribute: 'labels.zvi-face-recognition',
                modelId: MODEL_ID,
                values: { labels: ['Mark Ruffalo'] },
              },
            ]),
          ),
        },
      },
      `/${PROJECT_ID}/visualizer?id=${ASSET_ID}&query=W3sidHlwZSI6ImxhYmVsIiwiYXR0cmlidXRlIjoibGFiZWxzLnp2aS1mYWNlLXJlY29nbml0aW9uIiwibW9kZWxJZCI6IjYyMWJmNzc0LTg5ZDktMTI0NC05NTk2LWQ2ZGY0M2YxZWRlNSIsInZhbHVlcyI6eyJsYWJlbHMiOlsiTWFyayBSdWZmYWxvIl19fV0=`,
    )
  })

  it('should add a model/label filter in the asset view', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsMenu
        label={{
          modelId: MODEL_ID,
          scope: 'TRAIN',
          bbox: [0.226, 0.327, 0.697, 0.813],
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMKKKM...',
          b64_image: 'data:image/png;base64, iVBORw0KGgoAAAANSUhEU...',
        }}
        moduleName="zvi-face-recognition"
      />,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Select Add Filter
    act(() => {
      component.root
        .findByProps({ children: 'Add Model/Label Filter' })
        .props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: PROJECT_ID,
          id: ASSET_ID,
          query: btoa(
            JSON.stringify([
              {
                type: 'label',
                attribute: 'labels.zvi-face-recognition',
                modelId: MODEL_ID,
                values: { labels: ['Mark Ruffalo'] },
              },
            ]),
          ),
        },
      },
      `/${PROJECT_ID}/visualizer?id=${ASSET_ID}&query=W3sidHlwZSI6ImxhYmVsIiwiYXR0cmlidXRlIjoibGFiZWxzLnp2aS1mYWNlLXJlY29nbml0aW9uIiwibW9kZWxJZCI6IjYyMWJmNzc0LTg5ZDktMTI0NC05NTk2LWQ2ZGY0M2YxZWRlNSIsInZhbHVlcyI6eyJsYWJlbHMiOlsiTWFyayBSdWZmYWxvIl19fV0=`,
    )
  })

  it('should copy modelId', () => {
    const mockCopyFn = jest.fn()

    require('react-use-clipboard').__setMockCopyFn(mockCopyFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsMenu
        label={{
          modelId: MODEL_ID,
          scope: 'TRAIN',
          bbox: [0.226, 0.327, 0.697, 0.813],
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMKKKM...',
          b64_image: 'data:image/png;base64, iVBORw0KGgoAAAANSUhEU...',
        }}
        moduleName="zvi-face-recognition"
      />,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Copy Model ID
    act(() => {
      component.root.findByProps({ children: 'Copy Model ID' }).props.onClick()
    })

    expect(mockCopyFn).toHaveBeenCalledWith(MODEL_ID)
  })

  it('should copy simhash', () => {
    const mockCopyFn = jest.fn()

    require('react-use-clipboard').__setMockCopyFn(mockCopyFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsMenu
        label={{
          modelId: MODEL_ID,
          scope: 'TRAIN',
          bbox: [0.226, 0.327, 0.697, 0.813],
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMKKKM...',
          b64_image: 'data:image/png;base64, iVBORw0KGgoAAAANSUhEU...',
        }}
        moduleName="zvi-face-recognition"
      />,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Copy Simhash
    act(() => {
      component.root.findByProps({ children: 'Copy Simhash' }).props.onClick()
    })

    expect(mockCopyFn).toHaveBeenCalledWith('NNLHLINNMQPONMLMJFLMQMKKKM...')
  })
})
