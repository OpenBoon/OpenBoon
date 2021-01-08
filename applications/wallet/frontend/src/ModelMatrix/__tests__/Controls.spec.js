import TestRenderer, { act } from 'react-test-renderer'

import ModelMatrixControls from '../Controls'

const noop = () => () => {}

describe('<ModelMatrixControls', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <ModelMatrixControls
        settings={{
          isNormalized: true,
          defaultMin: 0,
          defaultMax: 1,
          minScore: 0,
          maxScore: 1,
        }}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not crash when min equals max', () => {
    const component = TestRenderer.create(
      <ModelMatrixControls
        settings={{
          isNormalized: true,
          defaultMin: 0,
          defaultMax: 0,
          minScore: 0,
          maxScore: 0,
        }}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should let a user input a min', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <ModelMatrixControls
        settings={{
          isNormalized: true,
          defaultMin: 0,
          defaultMax: 1,
          minScore: 0,
          maxScore: 1,
        }}
        dispatch={mockDispatch}
      />,
    )

    // do nothing when value is unchanged
    act(() => {
      component.root
        .findByProps({ value: 0 })
        .props.onBlur({ target: { value: 0 } })
    })

    expect(mockDispatch).not.toHaveBeenCalled()

    // cancel change when value is out of bound
    act(() => {
      component.root
        .findByProps({ value: 0 })
        .props.onChange({ target: { value: 100 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 100 })
        .props.onBlur({ target: { value: 100 } })
    })

    expect(mockDispatch).not.toHaveBeenCalled()

    // update when value is in bound
    act(() => {
      component.root
        .findByProps({ value: 0 })
        .props.onChange({ target: { value: 0.1 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 0.1 })
        .props.onKeyPress({ target: { value: 0.1 }, key: 'Not Enter' })
    })

    act(() => {
      component.root
        .findByProps({ value: 0.1 })
        .props.onKeyPress({ target: { value: 0.1 }, key: 'Enter' })
    })

    expect(mockDispatch).toHaveBeenCalledWith({ minScore: 0.1 })
  })

  it('should let a user input a max', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <ModelMatrixControls
        settings={{
          isNormalized: true,
          defaultMin: 0,
          defaultMax: 1,
          minScore: 0,
          maxScore: 1,
        }}
        dispatch={mockDispatch}
      />,
    )

    // do nothing when value is unchanged
    act(() => {
      component.root
        .findByProps({ value: 1 })
        .props.onBlur({ target: { value: 1 } })
    })

    expect(mockDispatch).not.toHaveBeenCalled()

    // cancel change when value is out of bound
    act(() => {
      component.root
        .findByProps({ value: 1 })
        .props.onChange({ target: { value: 100 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 100 })
        .props.onBlur({ target: { value: 100 } })
    })

    expect(mockDispatch).not.toHaveBeenCalled()

    // update when value is in bound
    act(() => {
      component.root
        .findByProps({ value: 1 })
        .props.onChange({ target: { value: 0.9 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 0.9 })
        .props.onKeyPress({ target: { value: 0.9 }, key: 'Not Enter' })
    })

    act(() => {
      component.root
        .findByProps({ value: 0.9 })
        .props.onKeyPress({ target: { value: 0.9 }, key: 'Enter' })
    })

    expect(mockDispatch).toHaveBeenCalledWith({ maxScore: 0.9 })
  })

  it('should let a user change view', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <ModelMatrixControls
        settings={{
          isNormalized: true,
          defaultMin: 0,
          defaultMax: 1,
          minScore: 0,
          maxScore: 1,
        }}
        dispatch={mockDispatch}
      />,
    )

    // Change view to Absolute
    act(() => {
      component.root
        .findByProps({ type: 'radio', value: 'absolute' })
        .props.onClick()
    })

    act(() => {
      component.root.findByType('form').props.onSubmit({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
