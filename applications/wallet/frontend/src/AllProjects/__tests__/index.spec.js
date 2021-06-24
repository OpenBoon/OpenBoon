import TestRenderer, { act } from 'react-test-renderer'

import projects from '../../Projects/__mocks__/projects'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import AllProjects from '..'

describe('<AllProjects />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: projects })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <AllProjects />
      </User>,
    )

    act(() => {
      component.root
        .findByProps({ label: 'Sort by' })
        .props.onChange({ value: 'date' })
    })

    expect(component.toJSON()).toMatchSnapshot()

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
        <AllProjects />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
