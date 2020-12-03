import TestRenderer from 'react-test-renderer'

import Button, { VARIANTS } from '..'

const variants = Object.keys(VARIANTS)
const hrefs = ['/hello', undefined]
const combinations = variants.reduce((accumulator, variant) => {
  const combination = hrefs.map((p) => [variant, p])
  return [...accumulator, ...combination]
}, [])

describe('<Button />', () => {
  combinations.forEach(([variant, href]) => {
    const linkOrButton = href ? 'Link' : 'Button'

    it(`should render properly for ${variant} ${linkOrButton}`, () => {
      const component = TestRenderer.create(
        <Button variant={variant} href={href}>
          Hello
        </Button>,
      )

      expect(component.toJSON()).toMatchSnapshot()
    })
  })

  it('should have a type "button", pass additional props, and be clickable', () => {
    const mockTestFunc = jest.fn()

    const component = TestRenderer.create(
      <Button variant={VARIANTS.PRIMARY} onClick={mockTestFunc}>
        Hello
      </Button>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    component.root.findByProps({ type: 'button' }).props.onClick()

    expect(mockTestFunc).toHaveBeenCalled()
  })

  it('should render a link as an anchor tag with no onClick', () => {
    const component = TestRenderer.create(
      <Button
        variant={VARIANTS.PRIMARY}
        href="http://www.aspiration.com"
        target="_blank"
      >
        Hello
      </Button>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    expect(component.root.findByType('a').props.onClick).toBeUndefined()
  })

  it('should have a type "button" and be aria-disabled, and not clickable', () => {
    const mockTestFunc = jest.fn()

    const component = TestRenderer.create(
      <Button variant={VARIANTS.PRIMARY} onClick={mockTestFunc} isDisabled>
        Hello
      </Button>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    component.root.findByProps({ type: 'button' }).props.onClick({
      preventDefault: () => {},
    })

    expect(mockTestFunc).not.toHaveBeenCalled()
  })
})
