import TestRenderer, { act } from 'react-test-renderer'

import AssetLabelingShortcuts from '../Shortcuts'

describe('<AssetLabelingShortcuts />', () => {
  it('should save a label', () => {
    const mockOnSubmit = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingShortcuts onSave={mockOnSubmit} />,
    )

    expect(component.toJSON()).toEqual(null)

    // useEffect
    act(() => {})

    // press shortcut for save
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'KeyS' })

      document.dispatchEvent(event)
    })

    expect(mockOnSubmit).toHaveBeenCalled()
  })

  it('should ignore other keys', () => {
    const mockOnSubmit = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingShortcuts onSave={mockOnSubmit} />,
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

    act(() => {
      component.unmount()
    })
  })
})
