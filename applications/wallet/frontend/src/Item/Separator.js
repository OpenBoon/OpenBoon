import { constants } from '../Styles'

const ItemSeparator = () => {
  return (
    <hr
      css={{
        width: '100%',
        border: 'none',
        borderBottom: constants.borders.regular.iron,
      }}
    />
  )
}

export default ItemSeparator
