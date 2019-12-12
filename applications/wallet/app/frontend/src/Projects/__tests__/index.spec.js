import TestRenderer from 'react-test-renderer'

import Projects from '..'

jest.mock('../../Layout')

describe('<Projects />', () => {
  it('should render properly without data', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Projects logout={mockFn}>{() => `Hello World`}</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with data', () => {
    const mockFn = jest.fn()
    require('swr').__setMockUseSWRResponse({
      data: { results: [{ id: '1', name: 'project-name' }] },
    })

    const component = TestRenderer.create(
      <Projects logout={mockFn}>{() => `Hello World`}</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
