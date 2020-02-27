import TestRenderer from 'react-test-renderer'

import ErrorBoundary from '..'

describe('<ErrorBoundary />', () => {
  it('should render children', () => {
    const component = TestRenderer.create(
      <ErrorBoundary>
        <div />
      </ErrorBoundary>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render generic when caught', () => {
    const spy = jest.spyOn(console, 'error')
    spy.mockImplementation(() => {})

    const Throw = () => {
      throw new Error('Error')
    }

    const component = TestRenderer.create(
      <ErrorBoundary>
        <Throw />
      </ErrorBoundary>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    spy.mockRestore()
  })
})
