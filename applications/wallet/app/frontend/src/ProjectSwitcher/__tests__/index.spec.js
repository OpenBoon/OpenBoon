import TestRenderer, { act } from 'react-test-renderer'

import ProjectSwitcher from '..'

import projects from '../../Projects/__mocks__/projects'

const noop = () => () => {}

describe('<ProjectSwitcher />', () => {
  it('should render properly without data', () => {
    const component = TestRenderer.create(
      <ProjectSwitcher projects={[]} setSelectedProject={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with data', () => {
    const mockFn = jest.fn()
    const mockProjects = projects.results.map(({ name }, index) => {
      return { id: `${index + 1}`, name, selected: index === 0 }
    })

    const component = TestRenderer.create(
      <ProjectSwitcher projects={mockProjects} setSelectedProject={mockFn} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('button')
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'asdf' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
    expect(mockFn).toHaveBeenCalledWith({ id: '2', name: 'asdf' })
  })
})
