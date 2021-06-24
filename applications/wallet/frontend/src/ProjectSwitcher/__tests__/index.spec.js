import TestRenderer, { act } from 'react-test-renderer'

import ProjectSwitcher from '..'

import projects from '../../Projects/__mocks__/projects'

const PROJECT_ID = projects.results[0].id

const noop = () => () => {}

describe('<ProjectSwitcher />', () => {
  it('should render properly without data', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(<ProjectSwitcher projectId="" />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with multiple projects by name', () => {
    require('next/router').__setUseRouter({
      asPath: `/${PROJECT_ID}/jobs`,
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <ProjectSwitcher projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('button')
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with multiple projects by date', () => {
    localStorage.setItem('AllProjectsContent.sortBy', `"date"`)

    require('next/router').__setUseRouter({
      asPath: `/${PROJECT_ID}/jobs`,
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: projects })

    const component = TestRenderer.create(
      <ProjectSwitcher projectId={PROJECT_ID} />,
    )

    act(() => {
      component.root
        .findByType('button')
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with one project', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...projects, count: 1, results: [projects.results[0]] },
    })

    const component = TestRenderer.create(
      <ProjectSwitcher projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not render if the projectId is not of an authorized project', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <ProjectSwitcher projectId="not-a-valid-project-id" />,
    )

    expect(component.toJSON()).toBeNull()
  })

  it('should not render if projectId is not in route', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: {},
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <ProjectSwitcher projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toBeNull()
  })
})
