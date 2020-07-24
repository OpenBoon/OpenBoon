import TestRenderer from 'react-test-renderer'

import ErrorBoundary, { VARIANTS } from '..'

describe('<ErrorBoundary />', () => {
  it('should render children', () => {
    const component = TestRenderer.create(
      <ErrorBoundary variant={VARIANTS.LOCAL}>
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
      <ErrorBoundary variant={VARIANTS.LOCAL}>
        <Throw />
      </ErrorBoundary>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    spy.mockRestore()
  })

  it('should render properly when transparent', () => {
    const spy = jest.spyOn(console, 'error')

    spy.mockImplementation(() => {})

    const Throw = () => {
      throw new Error('Error')
    }

    const component = TestRenderer.create(
      <ErrorBoundary variant={VARIANTS.LOCAL} isTransparent>
        <Throw />
      </ErrorBoundary>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    spy.mockRestore()
  })
})
