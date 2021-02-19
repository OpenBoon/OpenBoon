import TestRenderer, { act } from 'react-test-renderer'

import AssetLabelingMenu from '../Menu'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
const MODEL_ID = '621bf774-89d9-1244-9596-d6df43f1ede5'

const noop = () => () => {}

describe('<AssetLabelingMenu />', () => {
  beforeAll(() => {
    jest.useFakeTimers()
  })

  afterAll(() => {
    jest.useRealTimers()
  })

  it('should add a model/label filter', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(
      <AssetLabelingMenu
        modelId={MODEL_ID}
        label="Mark Ruffalo"
        scope="TRAIN"
        moduleName="boonai-face-recognition"
        triggerReload={noop}
        setError={noop}
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

    const query = btoa(
      JSON.stringify([
        {
          type: 'label',
          attribute: 'labels.boonai-face-recognition',
          modelId: MODEL_ID,
          values: { labels: ['Mark Ruffalo'] },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })

  it('should triggerReload on delete confirmation in the grid', async () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingMenu
        modelId={MODEL_ID}
        label="Mark Ruffalo"
        scope="TRAIN"
        moduleName="boonai-face-recognition"
        triggerReload={mockFn}
        setError={noop}
      />,
    )

    // Open menu
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Toggle Actions Menu' })[0]
        .props.onClick()
    })

    // Click Delete Label
    act(() => {
      component.root.findByProps({ children: 'Delete Label' }).props.onClick()
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Deleted' }))

    // Click Delete button in modal
    await act(async () => {
      component.root
        .findByProps({ children: 'Delete Permanently' })
        .props.onClick()
    })

    expect(mockFn).toHaveBeenCalled()
  })

  it('should triggerReload on delete confirmation in the asset view', async () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingMenu
        modelId={MODEL_ID}
        label="Mark Ruffalo"
        scope="TRAIN"
        moduleName="boonai-face-recognition"
        triggerReload={mockFn}
        setError={noop}
      />,
    )

    // Open menu
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Toggle Actions Menu' })[0]
        .props.onClick()
    })

    // Click Delete Label
    act(() => {
      component.root.findByProps({ children: 'Delete Label' }).props.onClick()
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Deleted' }))

    // Click Delete button in modal
    await act(async () => {
      component.root
        .findByProps({ children: 'Delete Permanently' })
        .props.onClick()
    })

    expect(mockFn).toHaveBeenCalled()
  })
})
