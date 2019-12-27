import TestRenderer from 'react-test-renderer'

import TableContent from '../Content'

const noop = () => () => {}

describe('<Table />', () => {
  it('should render properly without results', () => {
    const component = TestRenderer.create(
      <TableContent
        numColumns={1}
        isLoading={false}
        results={[]}
        renderEmpty="Empty"
        renderRow={noop}
        revalidate={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when loading', () => {
    const component = TestRenderer.create(
      <TableContent
        numColumns={1}
        isLoading
        results={[]}
        renderEmpty="Empty"
        renderRow={noop}
        revalidate={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly', () => {
    const component = TestRenderer.create(
      <TableContent
        numColumns={1}
        isLoading={false}
        results={[]}
        renderEmpty="Empty"
        renderRow={noop}
        revalidate={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
