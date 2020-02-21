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
    const MockFailedComponent = jest.fn(() => Promise.reject(new Error()))

    const component = TestRenderer.create(
      <ErrorBoundary>
        <MockFailedComponent />
      </ErrorBoundary>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
