import TestRenderer from 'react-test-renderer'

import Projects from '..'

import { projects } from '../__mocks__'

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
      data: { results: projects },
    })

    const component = TestRenderer.create(
      <Projects logout={mockFn}>{() => `Hello World`}</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
