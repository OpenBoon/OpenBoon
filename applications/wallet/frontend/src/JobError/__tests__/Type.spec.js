import TestRenderer from 'react-test-renderer'

import JobErrorType from '..'

describe('<JobErrorType />', () => {
  it('should render properly when fetching errors', () => {
    const component = TestRenderer.create(
      <JobErrorType
        error={{ message: 'this is an non-fatal error', fatal: false }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly', () => {
    const component = TestRenderer.create(
      <JobErrorType
        error={{ message: 'this is a fatal error', fatal: true }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
