import TestRenderer from 'react-test-renderer'

import options from '../__mocks__/options'

import ListboxOptions from '../Options'

describe('<ListboxOptions />', () => {
  it('should render a nested list properly', () => {
    const component = TestRenderer.create(
      <ListboxOptions options={options} nestedCount={0} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render a non-nested list properly', () => {
    const component = TestRenderer.create(
      <ListboxOptions
        options={{ fields: 'fields', media: 'media' }}
        nestedCount={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
