import TestRenderer, { act } from 'react-test-renderer'

import MetadataPrettyLabelsMenu from '../LabelsMenu'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('<MetadataPrettyLabelsMenu />', () => {
  it('should add a dataset/label filter', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsMenu
        label={{
          datasetId: DATASET_ID,
          scope: 'TRAIN',
          bbox: [0.226, 0.327, 0.697, 0.813],
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMKKKM...',
          b64Image: 'data:image/png;base64, iVBORw0KGgoAAAANSUhEU...',
        }}
        datasetName="boonai-face-recognition"
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
        .findByProps({ children: 'Add Dataset/Label Filter' })
        .props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'label',
          attribute: 'labels.boonai-face-recognition',
          datasetId: DATASET_ID,
          values: { labels: ['Mark Ruffalo'] },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })

  it('should copy datasetId', async () => {
    const mockCopyFn = jest.fn()

    window.navigator.clipboard.writeText = mockCopyFn

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsMenu
        label={{
          datasetId: DATASET_ID,
          scope: 'TRAIN',
          bbox: [0.226, 0.327, 0.697, 0.813],
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMKKKM...',
          b64Image: 'data:image/png;base64, iVBORw0KGgoAAAANSUhEU...',
        }}
        datasetName="boonai-face-recognition"
      />,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Copy Dataset ID
    await act(async () => {
      component.root
        .findByProps({ children: 'Copy Dataset ID' })
        .props.onClick()
    })

    expect(mockCopyFn).toHaveBeenCalledWith(DATASET_ID)
  })

  it('should copy simhash', async () => {
    const mockCopyFn = jest.fn()

    window.navigator.clipboard.writeText = mockCopyFn

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsMenu
        label={{
          datasetId: DATASET_ID,
          scope: 'TRAIN',
          bbox: [0.226, 0.327, 0.697, 0.813],
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMKKKM...',
          b64Image: 'data:image/png;base64, iVBORw0KGgoAAAANSUhEU...',
        }}
        datasetName="boonai-face-recognition"
      />,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Copy Simhash
    await act(async () => {
      component.root.findByProps({ children: 'Copy Simhash' }).props.onClick()
    })

    expect(mockCopyFn).toHaveBeenCalledWith('NNLHLINNMQPONMLMJFLMQMKKKM...')
  })
})
