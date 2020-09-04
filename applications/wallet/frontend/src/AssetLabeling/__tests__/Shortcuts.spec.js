import TestRenderer, { act } from 'react-test-renderer'

import AssetLabelingShortcuts from '../Shortcuts'

import assets from '../../Assets/__mocks__/assets'

const ASSET_ID = assets.results[0].id
const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../helpers')

const noop = () => {}

describe('<AssetLabelingShortcuts />', () => {
  it('should save a label', () => {
    const mockOnSubmit = jest.fn()

    require('../helpers').__setMockOnSubmit(mockOnSubmit)

    const component = TestRenderer.create(
      <AssetLabelingShortcuts
        dispatch={noop}
        state={{}}
        labels={[]}
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
      />,
    )

    expect(component.toJSON()).toEqual(null)

    // useEffect
    act(() => {})

    // press shortcut for save
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'KeyS' })

      document.dispatchEvent(event)
    })

    expect(mockOnSubmit).toHaveBeenCalledWith({
      dispatch: noop,
      state: {},
      labels: [],
      projectId: PROJECT_ID,
      assetId: ASSET_ID,
    })
  })

  it('should ignore other keys', () => {
    const mockOnSubmit = jest.fn()

    require('../helpers').__setMockOnSubmit(mockOnSubmit)

    const component = TestRenderer.create(
      <AssetLabelingShortcuts
        dispatch={noop}
        state={{}}
        labels={[]}
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
      />,
    )

    expect(component.toJSON()).toEqual(null)

    // useEffect
    act(() => {})

    // press key with no shortcut
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'KeyL' })

      document.dispatchEvent(event)
    })

    expect(mockOnSubmit).not.toHaveBeenCalled()
  })
})
