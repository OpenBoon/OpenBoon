import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import models from '../../Models/__mocks__/models'
import project from '../../Project/__mocks__/project'

import AssetLabelingMenu from '../Menu'

const PROJECT_ID = project.id
const ASSET_ID = asset.id
const MODEL_ID = models.results[0].id

const noop = () => () => {}

describe('<AssetLabelingMenu />', () => {
  it('should triggerReload on delete confirmation', async () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingMenu
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        queryString=""
        modelId={MODEL_ID}
        label=""
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
      component.root.findByProps({ title: 'Delete Label' }).props.onConfirm()
    })

    expect(mockFn).toHaveBeenCalled()
  })
})
