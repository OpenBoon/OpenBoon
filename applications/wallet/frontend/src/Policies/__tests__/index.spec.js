import TestRenderer, { act } from 'react-test-renderer'

import Policies from '..'

const noop = () => () => {}

describe('<Policies />', () => {
  it('should render properly', async () => {
    const mockMutate = jest.fn()

    require('swr').__setMockMutateFn(mockMutate)

    const component = TestRenderer.create(<Policies userId={42} />)

    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    // Mock Failure
    fetch.mockRejectOnce({ error: 'Invalid' }, { status: 400 })

    await act(async () => {
      component.root
        .findByProps({ children: 'Continue' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Success
    fetch.mockResponseOnce('{}')

    await act(async () => {
      component.root
        .findByProps({ children: 'Continue' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(fetch.mock.calls.length).toEqual(2)

    expect(fetch.mock.calls[0][0]).toEqual(`/api/v1/users/42/agreements/`)

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      body: '{"policiesDate":"20200414"}',
      method: 'POST',
    })

    expect(mockMutate).toHaveBeenCalledWith({
      agreedToPoliciesDate: '20200414',
    })
  })

  it('should not POST the form', () => {
    const mockFn = jest.fn()
    const mockOnSubmit = jest.fn()

    const component = TestRenderer.create(<Policies userId={42} />)

    component.root
      .findByProps({ method: 'post' })
      .props.onSubmit({ preventDefault: mockFn })

    expect(mockOnSubmit).not.toHaveBeenCalled()
    expect(mockFn).toHaveBeenCalled()
  })
})
