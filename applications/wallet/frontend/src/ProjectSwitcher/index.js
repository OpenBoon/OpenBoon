import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { spacing, colors } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

const CHEVRON_WIDTH = 20

const ProjectSwitcher = () => {
  const {
    pathname,
    query: { projectId },
  } = useRouter()

  const { data: { results: projects = [] } = {} } = useSWR('/api/v1/projects/')

  if (!projectId || !projects.length === 0) return null

  const selectedProject = projects.find(({ id }) => id === projectId)

  if (!selectedProject) return null

  if (projects.length === 1) {
    return (
      <div
        css={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: `${spacing.small}px ${spacing.base}px`,
          color: colors.key.one,
        }}>
        {selectedProject.name}
      </div>
    )
  }

  return (
    <Menu
      open="right"
      button={({ onBlur, onClick, isMenuOpen }) => (
        <Button
          variant={VARIANTS.MENU}
          onBlur={onBlur}
          onClick={onClick}
          isDisabled={false}>
          <div
            css={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
            }}>
            {selectedProject.name}
            <ChevronSvg
              width={CHEVRON_WIDTH}
              css={{
                marginLeft: spacing.base,
                transform: `${isMenuOpen ? 'rotate(-180deg)' : ''}`,
              }}
            />
          </div>
        </Button>
      )}>
      {({ onBlur, onClick }) => (
        <ul>
          {projects.map(({ id, name }) => (
            <li key={id}>
              <Link
                href={pathname}
                as={pathname.replace('[projectId]', id)}
                passHref>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={onClick}
                  isDisabled={false}>
                  {name}
                </Button>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Menu>
  )
}

export default ProjectSwitcher
