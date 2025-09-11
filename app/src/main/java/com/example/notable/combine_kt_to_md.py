#
#  combine_kt_to_md.py
#
#  This script finds all .kt files in a specified directory and its subdirectories,
#  and combines them into a single Markdown file.
#
#  Author: Gemini 2.5 Pro
#  Date: 1404/06/20 (2025/09/11)
#

import os
import argparse

def find_kotlin_files(root_dir):
    """
    Recursively finds all files with the .kt extension in a directory.

    Args:
        root_dir (str): The path to the root directory to start the search from.

    Returns:
        list: A sorted list of full paths to the .kt files.
    """
    kotlin_files = []
    print(f"🔍 Starting search for .kt files in '{root_dir}'...")

    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith(".kt"):
                full_path = os.path.join(dirpath, filename)
                kotlin_files.append(full_path)

    # Sort the files alphabetically for a consistent order
    kotlin_files.sort()
    print(f"✅ Found {len(kotlin_files)} Kotlin files.")
    return kotlin_files

def combine_files_to_markdown(file_list, root_dir, output_filename):
    """
    Combines the content of files into a single Markdown file.

    Args:
        file_list (list): A list of full paths to the files to combine.
        root_dir (str): The original root directory, used to create relative paths.
        output_filename (str): The name of the output Markdown file.
    """
    print(f"✍️ Writing content to '{output_filename}'...")
    try:
        with open(output_filename, 'w', encoding='utf-8') as md_file:
            md_file.write(f"# Combined Kotlin Code\n\n")
            md_file.write(f"Source Directory: `{root_dir}`\n\n")
            md_file.write(f"Date Generated: 1404/06/20 (2025/09/11)\n\n")
            md_file.write("---\n\n")

            if not file_list:
                md_file.write("No Kotlin (.kt) files were found in the specified directory.\n")
                print("⚠️ No .kt files found to combine.")
                return

            for index, file_path in enumerate(file_list):
                # os.path.relpath makes the path cleaner and relative to the start directory
                relative_path = os.path.relpath(file_path, root_dir)
                # Use forward slashes for paths in markdown for better compatibility
                relative_path_for_md = relative_path.replace(os.sep, '/')

                print(f"  -> Processing ({index + 1}/{len(file_list)}): {relative_path}")

                md_file.write(f"## File: `{relative_path_for_md}`\n\n")

                try:
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as kt_file:
                        content = kt_file.read()
                        md_file.write("```kotlin\n")
                        md_file.write(content.strip() + "\n")
                        md_file.write("```\n\n")
                        md_file.write("---\n\n")
                except Exception as e:
                    error_message = f"Error reading file {file_path}: {e}"
                    print(f"  ❌ {error_message}")
                    md_file.write(f"```\n[ERROR: Could not read file content. Reason: {e}]\n```\n\n---\n\n")

        print(f"\n🎉 Success! All Kotlin files have been combined into '{output_filename}'.")

    except IOError as e:
        print(f"🔥 Critical Error: Could not write to the output file '{output_filename}'. Reason: {e}")
        print("Please check your file permissions.")

def main():
    """Main function to run the script."""
    parser = argparse.ArgumentParser(
        description="A utility to combine all Kotlin (.kt) files from a directory and its subdirectories into a single Markdown file.",
        epilog="Example: python combine_kt_to_md.py -d /path/to/my/kotlin_project -o my_project_code.md"
    )

    parser.add_argument(
        '-d', '--directory',
        type=str,
        required=True,
        help="The root directory of your Kotlin project to search for .kt files."
    )

    parser.add_argument(
        '-o', '--output',
        type=str,
        default="combined_kotlin_code.md",
        help="The name of the output Markdown file. Defaults to 'combined_kotlin_code.md'."
    )

    args = parser.parse_args()

    source_directory = args.directory
    output_file = args.output

    if not os.path.isdir(source_directory):
        print(f"🔥 Error: The specified directory '{source_directory}' does not exist.")
        return

    # Normalize the path to handle things like 'src/./'
    source_directory = os.path.abspath(source_directory)

    kotlin_files_found = find_kotlin_files(source_directory)
    if kotlin_files_found:
        combine_files_to_markdown(kotlin_files_found, source_directory, output_file)
    else:
        # Create an empty or informational markdown file even if no kt files are found
        combine_files_to_markdown([], source_directory, output_file)


if __name__ == "__main__":
    main()
