import TestRenderer, { act } from 'react-test-renderer'

import projects from '../../Projects/__mocks__/projects'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Account from '..'

describe('<Account />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: projects })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Account />
      </User>,
    )

    act(() => {
      component.root
        .findByProps({ type: 'search' })
        .props.onChange({ target: { value: 'erty' } })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no projects', () => {
    require('swr').__setMockUseSWRResponse({ data: { results: [] } })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Account />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
