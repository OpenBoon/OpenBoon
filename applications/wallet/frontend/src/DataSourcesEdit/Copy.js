import { constants, colors } from '../Styles'

const DataSourcesEditCopy = () => {
  return (
    <p
      css={{
        marginTop: 0,
        maxWidth: constants.paragraph.maxWidth,
        color: colors.structure.zinc,
      }}
    >
      Additional file types or analysis modules can be added to this data
      source. All previous selections will remain constant and cannot be
      modified.
    </p>
  )
}

export default DataSourcesEditCopy
