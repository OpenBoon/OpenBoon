import TestRenderer, { act } from 'react-test-renderer'

import ProjectSwitcher from '..'

import projects from '../../Projects/__mocks__/projects'

const noop = () => () => {}

describe('<ProjectSwitcher />', () => {
  it('should render properly without data', () => {
    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(<ProjectSwitcher />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with data', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: projects.results[0].id },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(<ProjectSwitcher />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('button')
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not render if the projectId is not of an authorized project', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: 'not-a-valid-project-id' },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(<ProjectSwitcher />)

    expect(component.toJSON()).toBeNull()
  })
})
