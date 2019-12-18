import TestRenderer, { act } from 'react-test-renderer'

import ApiKeysAddForm from '../Form'

const noop = () => () => {}

describe('<ApiKeysAddForm />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(<ApiKeysAddForm onSubmit={mockFn} />)

    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'API Key Name' } })
    })

    act(() => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith({ name: 'API Key Name' })
  })

  it('should not POST the form', () => {
    const mockFn = jest.fn()
    const mockOnSubmit = jest.fn()

    const component = TestRenderer.create(
      <ApiKeysAddForm onSubmit={mockOnSubmit} />,
    )

    component.root
      .findByProps({ method: 'post' })
      .props.onSubmit({ preventDefault: mockFn })

    expect(mockOnSubmit).not.toHaveBeenCalled()
    expect(mockFn).toHaveBeenCalled()
  })
})
